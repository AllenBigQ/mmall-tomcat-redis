package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Shipping;

/**
 * Created by Allen
 */
public  interface  IShippingService {
    //添加地址
    ServerResponse add(Integer userId, Shipping shipping);
    ServerResponse<String >del(Integer userId,Integer shippingId);
    ServerResponse update(Integer userId, Shipping shipping);
    ServerResponse<Shipping>select(Integer userId,Integer shippingId);
    ServerResponse<PageInfo>list(Integer userId, int pageNum, int pageSize);
}