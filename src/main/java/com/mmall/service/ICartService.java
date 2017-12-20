package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.vo.CartVo;

/**
 * Created by Allen
 */
public interface ICartService {
    //购物车添加商品
    ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count);
    //更新购物车某个产品数量
    ServerResponse<CartVo>update(Integer userId,Integer productId,Integer count);
    //移除购物车某个产品
    ServerResponse<CartVo>deleteProduct(Integer userId,String productIds);
    //获取购物车list
    ServerResponse<CartVo>list(Integer userId);
    //购物车选中或者取消某个产品
    ServerResponse<CartVo>selectOrUnselect(Integer userId,Integer productId,Integer checked);
    //查询购物车产品数量
    ServerResponse<Integer> getCartProductCount(Integer userId);
}
