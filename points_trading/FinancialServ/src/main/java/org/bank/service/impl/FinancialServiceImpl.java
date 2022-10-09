package org.bank.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.bank.constants.Constant;
import org.bank.entity.Financial;
import org.bank.exception.BusinessException;
import org.bank.exception.code.BaseResponseCode;
import org.bank.lock.redisson.RedissonLock;
import org.bank.mapper.FinancialMapper;
import org.bank.service.FinancialService;
import org.bank.service.RedisService;
import org.bank.utils.PageUtils;
import org.bank.vo.req.FinancialAddReqVO;
import org.bank.vo.req.FinancialDeductStockReqVO;
import org.bank.vo.req.FinancialPageReqVO;
import org.bank.vo.req.FinancialUpdateReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FinancialServiceImpl implements FinancialService {
    @Autowired
    FinancialMapper financialMapper;

    @Autowired
    RedisService redisService;

    @Autowired
    RedissonLock redissonLock;

    @Override
    public Financial addFinancial(FinancialAddReqVO vo) {
        //查询是否有此商品
        List<Financial> list = financialMapper.selectByName(vo.getName());
        Financial addFinancial = new Financial();
        BeanUtils.copyProperties(vo, addFinancial);
        addFinancial.setId("financial_" + UUID.randomUUID().toString());
        addFinancial.setCreatetime(new Date());
        if (list != null && list.size() > 0) {
            throw new BusinessException(BaseResponseCode.FINANCIAL_ISEXIT);
        }
        int insertSize = financialMapper.insertSelective(addFinancial);
        if (insertSize <= 0) {
            throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
        }
        redisService.set(Constant.FINANCIAL_CACHE_KEY + addFinancial.getId(), addFinancial);
        return addFinancial;
    }

    @Override
    public Integer getStock(String financialId) {
        try {
            //获取锁，锁定库存
            if (redissonLock.lock(Constant.FINANCIAL_STOCK_LOCK_KEY+ financialId, 10)) {
                //先取商品
                Financial financial = null;
                if (redisService.hasKey(Constant.FINANCIAL_CACHE_KEY + financialId)) {
                    financial = JSONObject.parseObject(redisService.get(Constant.FINANCIAL_CACHE_KEY + financialId).toString(),Financial.class);
                } else {
                    financial = detailInfo(financialId);
                }
                if (financial == null) {
                    throw new BusinessException(BaseResponseCode.FINANCIAL_ISNOTEXIT);
                }
                //返回库存
                Integer lockStockNum = redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId) == null ? 0 : Integer.parseInt(redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId).toString());
                Integer financialStock = financial.getStock()==null?0:financial.getStock();
                return financialStock-lockStockNum;
            } else {
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        } catch (Exception e) {
            if(e instanceof  BusinessException){
                throw e;
            }else{
                log.info("获取商品库存错误,错误信息为{}",e.getMessage());
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }finally {
            //释放锁
            redissonLock.release(Constant.FINANCIAL_STOCK_LOCK_KEY + financialId);
        }
    }

    @Override
    public boolean lockStock(Integer stock, String financialId) {
        if (stock == null || stock == 0) {
            return true;
        }
        try {
            //获取锁，锁定库存
            if (redissonLock.lock(Constant.FINANCIAL_STOCK_LOCK_KEY + financialId, 10)) {
                //先取商品
                Financial financial = null;
                if (redisService.hasKey(Constant.FINANCIAL_CACHE_KEY + financialId)) {
                    financial = JSONObject.parseObject(redisService.get(Constant.FINANCIAL_CACHE_KEY + financialId).toString(),Financial.class);
                } else {
                    financial = detailInfo(financialId);
                }
                if (financial == null) {
                    throw new BusinessException(BaseResponseCode.FINANCIAL_ISNOTEXIT);
                }
                //判断库存是否满足
                Integer lockStockNum = redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId) == null ? 0 : Integer.parseInt(redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId).toString());
                Integer financialStock = financial.getStock()==null?0:financial.getStock();
                if (lockStockNum + stock <= financialStock) {
                    redisService.set(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId, lockStockNum + stock);
                    return true;
                } else {
                    throw new BusinessException(BaseResponseCode.FINANCIAL_STOCK_IS_OUT);
                }
            } else {
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }catch (Exception e) {
            if(e instanceof  BusinessException){
                throw e;
            }else{
                log.info("锁定商品库存错误,错误信息为{}",e.getMessage());
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }finally {
            //释放锁
            redissonLock.release(Constant.FINANCIAL_STOCK_LOCK_KEY+ financialId);
        }
    }

    @Override
    public boolean unLockStock(Integer stock, String financialId) {
        try {
            //获取锁，解锁库存
            if (redissonLock.lock(Constant.FINANCIAL_STOCK_LOCK_KEY + financialId, 10)) {
                Integer lockStockNum = redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId) == null ? 0 : Integer.parseInt(redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId).toString());
                if (stock < lockStockNum) {
                    redisService.set(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId, lockStockNum - stock);
                } else {
                    redisService.set(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId, 0);
                }
                return true;
            } else {
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }catch (Exception e) {
            if(e instanceof  BusinessException){
                throw e;
            }else{
                log.info("释放商品库存错误,错误信息为{}",e.getMessage());
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }finally {
            //释放锁
            redissonLock.release(Constant.FINANCIAL_STOCK_LOCK_KEY + financialId);
        }
    }

    @Override
    public void updateFinancial(FinancialUpdateReqVO vo) {
        Financial financial = financialMapper.selectByPrimaryKey(vo.getId());
        if (financial == null) {
            log.error("传入 的 id:{}不合法", vo.getId());
            throw new BusinessException(BaseResponseCode.DATA_ERROR);
        }
        List<Financial> list = financialMapper.selectByName(vo.getName());
        if (list != null && list.size() > 0 && !financial.getId().equals(list.get(0).getId())) {
            throw new BusinessException(BaseResponseCode.FINANCIAL_ISEXIT);
        }
        Financial update = new Financial();
        BeanUtils.copyProperties(vo, update);
        update.setUpdatetime(new Date());
        int count = financialMapper.updateByPrimaryKeySelective(update);
        if (count != 1) {
            throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
        }
        //重置
        //更新缓存
        redisService.set(Constant.FINANCIAL_CACHE_KEY + vo.getId(), detailInfo(vo.getId()));
    }

    @Override
    public void deductStock(FinancialDeductStockReqVO vo) {
        //获取商品信息
        Financial financial=detailInfo(vo.getId());
        if(financial==null){
            throw  new  BusinessException(BaseResponseCode.FINANCIAL_ISNOTEXIT);
        }
        String financialId=financial.getId();
        try {
            Integer deductStock=vo.getStock();
            //获取锁，解锁库存
            if (redissonLock.lock(Constant.FINANCIAL_STOCK_LOCK_KEY + financialId, 10)) {
                Integer financialStock=financial.getStock()==null?0:financial.getStock();
                if(financialStock-deductStock<0){
                    throw  new  BusinessException(BaseResponseCode.FINANCIAL_STOCK_IS_OUT);
                }
                Financial update=new Financial();
                update.setId(financialId);
                update.setStock(financialStock-deductStock);
                update.setUpdatetime(new Date());
                int updatesize=financialMapper.updateByPrimaryKeySelective(update);
                if(updatesize==0){
                    throw  new  BusinessException(BaseResponseCode.PAY_ORDER_STOCK_DEDUCT_ERRPR);
                }
                Integer lockStockNum = redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId) == null ? 0 : Integer.parseInt(redisService.get(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId).toString());
                if (deductStock < lockStockNum) {
                    redisService.set(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId, lockStockNum - deductStock);
                } else {
                    redisService.set(Constant.FINANCIAL_STOCK_LOCKED_KEY + financialId, 0);
                }
                //重置缓存
                financial.setStock(financialStock-deductStock);
                redisService.set(Constant.FINANCIAL_CACHE_KEY + financialId,financial);
            } else {
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }  catch (Exception e) {
            log.info("商品库存扣除错误,错误信息为{}",e.getMessage());
            if(e instanceof  BusinessException){
                throw e;
            }else{
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }finally {
            //释放锁
            redissonLock.release(Constant.FINANCIAL_STOCK_LOCK_KEY + financialId);
        }
    }

    @Override
    public Financial detailInfo(String id) {
        if(redisService.hasKey(Constant.FINANCIAL_CACHE_KEY+id)){
            return  JSONObject.parseObject(redisService.get(Constant.FINANCIAL_CACHE_KEY + id).toString(),Financial.class);
        }
        Financial financial = financialMapper.selectByPrimaryKey(id);
        if (financial == null) {
            log.error("传入 的 id:{}不合法", id);
            throw new BusinessException(BaseResponseCode.DATA_ERROR);
        }
        return financial;
    }

    @Override
    public void deletedFinancial(String id) {
        Financial financial = financialMapper.selectByPrimaryKey(id);
        if (financial == null) {
            log.error("传入 的 id:{}不合法", id);
            throw new BusinessException(BaseResponseCode.DATA_ERROR);
        }
        Financial update = new Financial();
        update.setId(id);
        update.setDeleted(0);
        update.setUpdatetime(new Date());
        int updateSize = financialMapper.updateByPrimaryKeySelective(update);
        if (updateSize <= 0) {
            throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
        }
        //更新缓存
        if (redisService.hasKey(Constant.FINANCIAL_CACHE_KEY + id)) {
            redisService.delete(Constant.FINANCIAL_CACHE_KEY + id);
        }
    }

    @Override
    public PageVO<Financial> pageInfo(FinancialPageReqVO vo) {
        PageHelper.startPage(vo.getPageNum(), vo.getPageSize());
        Financial query = new Financial();
        BeanUtils.copyProperties(vo, query);
        List<Financial> financial = financialMapper.selectAll(query);
        return PageUtils.getPageVO(financial);
    }

    @Override
    public List<Financial> selectAllFinancial() {
        return financialMapper.selectAll(null);
    }
}
