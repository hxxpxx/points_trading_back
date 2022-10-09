package org.bank.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.bank.client.AddressClient;
import org.bank.client.GoodsClient;
import org.bank.constants.Constant;
import org.bank.entity.*;
import org.bank.exception.BusinessException;
import org.bank.exception.code.BaseResponseCode;
import org.bank.lock.redisson.RedissonLock;
import org.bank.mapper.OrderMapper;
import org.bank.message.OrderDelayMessageVO;
import org.bank.message.OrderPayMessageVO;
import org.bank.service.*;
import org.bank.utils.DataResult;
import org.bank.utils.PageUtils;
import org.bank.vo.req.*;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderMapper orderMapper;

    @Autowired
    RedisService redisService;

    @Autowired
    RedissonLock redissonLock;


    @Autowired
    AddressClient addressClient;

    @Autowired
    GoodsClient goodsClient;

    @Autowired
    OrderAddressService orderAddressService;

    @Autowired
    OrderSnapshotService orderSnapshotService;

    @Autowired
    MsgSenderService msgSenderService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private long redisTimeOut=1000*60*60;

    /**
     * 签发订单号给页面
     *
     * @return
     */
    @Override
    public String issueOrderNo() {
        //获取分布式锁
        try {
            if (redissonLock.lock("getOrderNoLock", 10)) {
                String orderNo = "order_" + UUID.randomUUID().toString();
                return orderNo;
            } else {
                throw new BusinessException(BaseResponseCode.GET_ORDERNO_AWAIT);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            //释放锁
            redissonLock.release("getOrderNoLock");
        }

    }

    @Override
    public boolean cancelOrderNo(String orderNo) {
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    /**
     * 创建订单
     * @param vo
     * @return
     */
    @Override
    public Order addOrder(OrderAddReqVO vo, String userId) {
        String orderNo = vo.getOrderNo();
        //获取锁，消费订单号
        Order addOrder=null;
        boolean isUnLockStock=false;
        //获取分布式锁
        try {
            if (redissonLock.lock(Constant.ORDERNO_LOCK_KEY + orderNo, 10)) {
                //如果没有商品ID  直接返回
                if(org.apache.commons.lang3.StringUtils.isBlank(vo.getGoodsId())){
                    throw new BusinessException(BaseResponseCode.GET_ORDER_GOODS_ERROR);
                }
                if(vo.getNum()==null||vo.getNum()<=0){
                    throw new BusinessException(BaseResponseCode.GET_ORDER_NUM_ERROR);
                }
                addOrder = new Order();
                BeanUtils.copyProperties(vo, addOrder);
                //组装数据
                addOrder = makerderData(addOrder);
                addOrder.setUserId(userId);
                //数据校验
                checkOrderData(addOrder);
                int insertSize = 0;
                insertSize = orderMapper.insert(addOrder);
                if (insertSize <= 0) {
                    throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
                }
                //交易快照
                orderSnapshotService.addOrderSnapshot(addOrder.getOrderSnapshot());
                //订单收获地址
                orderAddressService.addOrderAddress(addOrder.getOrderAddress());
                //锁定库存
                GoodsStockReqVO goodsStockReqVO = new GoodsStockReqVO();
                goodsStockReqVO.setGoodsId(addOrder.getGoodsId());
                goodsStockReqVO.setStackNum(addOrder.getNum());
                DataResult<Boolean> goodsStockLockResult = goodsClient.lockStock(goodsStockReqVO);
                if (goodsStockLockResult.getCode()!=0) {
                    log.info("订单创建失败，失败原因为{}", goodsStockLockResult.getMsg());
                    throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
                }else{
                    isUnLockStock=true;
                }
                //发送延时消息
                //参数一：topic   如果想添加tag,可以使用"topic:tag"的写法
                //参数二：Message<?>
                //参数三：消息发送超时时间
                //参数四：delayLevel 延时level  messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                OrderDelayMessageVO orderDelayMessageVO=new OrderDelayMessageVO();
                orderDelayMessageVO.setOrderNo(vo.getOrderNo());
                SendResult sentResult= rocketMQTemplate.syncSend(String.format("%s:%s", Constant.ORDER_MQ_TOPIC_KEY, Constant.ORDER_DELAY_PAY_TAG_KEY), MessageBuilder.withPayload(JSONObject.toJSONString(orderDelayMessageVO)).build(),3000,16);
                return addOrder;
            }else{
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }catch (Exception e){
            log.error("创建订单异常，异常原因为{}",e.getMessage());
            if(isUnLockStock){
                GoodsStockReqVO goodsStockReqVO=new GoodsStockReqVO();
                goodsStockReqVO.setStackNum(addOrder.getNum());
                goodsStockReqVO.setGoodsId(addOrder.getGoodsId());
                goodsClient.unLockStock(goodsStockReqVO);
//              msgSenderService.sendMessage(Constant.ORDER_MQ_TOPIC_KEY,Constant.ORDER_CANCEL_TAG_KEY, JSONObject.toJSONString(goodsStockReqVO));
            }
            if(e instanceof  BusinessException){
                throw e;
            }else{
                throw new BusinessException(BaseResponseCode.GET_ORDER_CREATE_ERROR);
            }

        }finally {
            redissonLock.release(Constant.ORDERNO_LOCK_KEY + orderNo);
        }

    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    @Override
    public void cancleOrder(String orderNo) {
        //校验订单号
        if(StringUtils.isNotBlank(orderNo)){
            Order orderInfo= detailInfo(orderNo);
            //只有未支付的单子可以取消订单操作
            if(orderInfo!=null&&StringUtils.isNotBlank(orderInfo.getGoodsId())&&"unpaid".equals(orderInfo.getStatus())){
                //取消订单
                Order update=new Order();
                update.setOrderNo(orderNo);
                update.setStatus("cancel");
                update.setUpdatetime(new Date());
                int updateSize=orderMapper.updateByPrimaryKeySelective(update);

                if(updateSize>0){
                    //解锁商品库存
                    GoodsStockReqVO goodsStockReqVO=new GoodsStockReqVO();
                    goodsStockReqVO.setStackNum(orderInfo.getNum());
                    goodsStockReqVO.setGoodsId(orderInfo.getGoodsId());
                    msgSenderService.sendMessage(Constant.ORDER_MQ_TOPIC_KEY,Constant.ORDER_CANCEL_TAG_KEY, JSONObject.toJSONString(goodsStockReqVO));
                }else{
                    throw new BusinessException(BaseResponseCode.CANCLE_ORDER_ERROR);
                }
            }else{
                throw new BusinessException(BaseResponseCode.CANCLE_ORDER_STATUS_ERROR);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    @Override
    public void cancleOrder(String orderNo,String userId) {
        //校验订单号
        if(StringUtils.isNotBlank(orderNo)){
            Order orderInfo= detailInfo(orderNo);
            if(StringUtils.isBlank(userId)||!userId.equals(orderInfo.getUserId())){
                throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
            }
            //只有未支付的单子可以取消订单操作
            if(orderInfo!=null&&StringUtils.isNotBlank(orderInfo.getGoodsId())&&"unpaid".equals(orderInfo.getStatus())){
                //取消订单
                Order update=new Order();
                update.setOrderNo(orderNo);
                update.setStatus("cancel");
                update.setUpdatetime(new Date());
                int updateSize=orderMapper.updateByPrimaryKeySelective(update);

                if(updateSize>0){
                    //解锁商品库存
                    GoodsStockReqVO goodsStockReqVO=new GoodsStockReqVO();
                    goodsStockReqVO.setStackNum(orderInfo.getNum());
                    goodsStockReqVO.setGoodsId(orderInfo.getGoodsId());
                    msgSenderService.sendMessage(Constant.ORDER_MQ_TOPIC_KEY,Constant.ORDER_CANCEL_TAG_KEY, JSONObject.toJSONString(goodsStockReqVO));
                }else{
                    throw new BusinessException(BaseResponseCode.CANCLE_ORDER_ERROR);
                }
            }else{
                throw new BusinessException(BaseResponseCode.CANCLE_ORDER_STATUS_ERROR);
            }
        }
    }

    @Override
    public void payOrder(String orderNo,String userId) {
        //校验订单号
        try{
            if(StringUtils.isNotBlank(orderNo)) {
                Order orderInfo = detailInfo(orderNo);
                if (orderInfo == null || StringUtils.isBlank(orderInfo.getGoodsId())) {
                    throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ORDERNO_ERRPR);
                }
                if(orderInfo.getUserId()==null||!orderInfo.getUserId().equals(userId)){
                    //非本人结束操作
                    throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
                }
                //判断订单状态
                if(!"unpaid".equals(orderInfo.getStatus())){
                    throw new BusinessException(BaseResponseCode.PAY_ORDER_STATUS_ERRPR);
                }
                //订单支付
                //1  修改订单状态为支付中
                Order update = new Order();
                update.setOrderNo(orderNo);
                update.setStatus("paying");
                update.setUpdatetime(new Date());
                int updateSize = orderMapper.updateByPrimaryKeySelective(update);
                if (updateSize > 0) {
                    //2发送事务消息  扣除积分与库存
                    OrderPayMessageVO orderPayMessageVO=new OrderPayMessageVO();
                    BeanUtils.copyProperties(orderInfo,orderPayMessageVO);
                    TransactionSendResult transactionSendResult = rocketMQTemplate.sendMessageInTransaction
                            (String.format("%s:%s", Constant.ORDER_MQ_TOPIC_KEY, Constant.ORDER_PAY_TAG_KEY), MessageBuilder.withPayload(orderPayMessageVO).build(), orderPayMessageVO);
                }else{
                    throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ERRPR);
                }
            }else{
                throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ORDERNO_ERRPR);
            }
        }catch (Exception e){
            throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ERRPR);
        }

    }

    @Override
    public void updateOrder(OrderUpdateReqVO vo) {
        String orderNo=vo.getOrderNo();
        if(StringUtils.isNotBlank(orderNo)){
            Order orderInfo=detailInfo(orderNo);
            if(orderInfo==null||StringUtils.isBlank(orderInfo.getGoodsId())){
                return;
            }
            Order update=new Order();
            BeanUtils.copyProperties(vo,update);
            update.setUpdatetime(new Date());
            int updateSize=orderMapper.updateByPrimaryKeySelective(update);
            if(updateSize==0){
                throw  new BusinessException(BaseResponseCode.OPERATION_ERRO);
            }
        }
    }

    /**
     * @param order
     * @return
     */
    private boolean checkOrderData(Order order) {
        //校验订单号
        Order checkOrder = detailInfo(order.getOrderNo());
        if (checkOrder != null) {
            throw new BusinessException(BaseResponseCode.GET_ORDERNO_ISEXIT);
        }
        //校验商品
        if (order.getGoods() == null || StringUtils.isEmpty(order.getTitle())) {
            throw new BusinessException(BaseResponseCode.GET_ORDER_GOODS_ERROR);
        }
        return true;
    }

    //组装数据
    private Order makerderData(Order order) {
        Date createTime = new Date();
        order.setCreatetime(createTime);
        order.setStatus("unpaid");
        //收获地址信息
        Integer addressId = order.getAddressId();
        if (addressId != null && addressId != 0) {
            DataResult<Address> addressDataResult = addressClient.detailInfo(addressId);
            if (addressDataResult.getCode()==0 && addressDataResult.getData() != null) {
                Address address = addressDataResult.getData();
                OrderAddress orderAddress = new OrderAddress();
                orderAddress.setAddress(address.getAddress());
                orderAddress.setContacts(address.getContacts());
                orderAddress.setPhone(address.getPhone());
                orderAddress.setOrderNo(order.getOrderNo());
                orderAddress.setCreatetime(createTime);
                order.setOrderAddress(orderAddress);
            }
        }
        //商品信息
        String goodsId = order.getGoodsId();
        if (!StringUtils.isEmpty(goodsId)) {
            DataResult<Goods> goodsDataResult = goodsClient.detailInfo(goodsId);
            if (goodsDataResult.getCode()==0 && goodsDataResult.getData() != null) {
                Goods goods = goodsDataResult.getData();
                order.setGoods(goods);
                order.setTitle(goods.getName());
                Double price = goods.getPrice() == null ? 0.00 : goods.getPrice();
                Double totalAmout = null;
                BigDecimal priceDc = new BigDecimal(price.toString());
                BigDecimal numDc = new BigDecimal((order.getNum() == null ? new Integer(0) : order.getNum()).toString());
                totalAmout = priceDc.multiply(numDc).doubleValue();
                order.setTotalAmout(totalAmout);
                //交易快照信息
                OrderSnapshot orderSnapshot = new OrderSnapshot();
                orderSnapshot.setOrderNo(order.getOrderNo());
                orderSnapshot.setCreatetime(createTime);
                orderSnapshot.setGoodsId(goodsId);
                orderSnapshot.setTitle(goods.getName());
                orderSnapshot.setDescribe(goods.getDescribe());
                orderSnapshot.setPrice(goods.getPrice());
                order.setOrderSnapshot(orderSnapshot);
            }
        }
        return order;
    }


    @Override
    public Order detailInfo(String orderNo,String userId) {
        Order order = orderMapper.selectByPrimaryKey(orderNo);
        if(StringUtils.isBlank(userId)||!userId.equals(order.getUserId())){
            throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
        }
        return order;
    }
    @Override
    public Order detailInfo(String orderNo) {
        Order order = orderMapper.selectByPrimaryKey(orderNo);
        return order;
    }



    @Override
    public PageVO<Order> pageInfo(OrderPageReqVO vo) {
        Order query=new Order();
        BeanUtils.copyProperties(vo,query);
        PageHelper.startPage(vo.getPageNum(), vo.getPageSize());
        BeanUtils.copyProperties(vo, query);
        List<Order> orderList = orderMapper.selectAll(query);
        return PageUtils.getPageVO(orderList);
    }

    @Override
    public OrderAddress getAddressForOrderNo(String orderNo) {
        return orderAddressService.detailInfo(orderNo);
    }

    @Override
    public OrderSnapshot getOrderSnapshotForOrderNo(String orderNo) {
        return orderSnapshotService.detailInfo(orderNo);
    }

}
