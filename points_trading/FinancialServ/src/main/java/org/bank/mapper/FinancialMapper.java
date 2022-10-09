package org.bank.mapper;

import org.bank.entity.Financial;

import java.util.List;

public interface FinancialMapper {
    int deleteByPrimaryKey(String id);

    int insert(Financial record);

    int insertSelective(Financial record);

    Financial selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Financial record);

    int updateByPrimaryKey(Financial record);

    List<Financial> selectAll(Financial vo);

    List<Financial> selectByName(String name);
}