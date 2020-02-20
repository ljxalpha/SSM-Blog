package service;

import po.Page;
import po.WeiboCustom;

public interface WeiboLuceneService {
    //Lucene版本的根据关键字搜索微博
    public Page<WeiboCustom> queryWeiboByWordWithLucene(String keyWord, int pageNo) throws Exception;

    //删除微博时，同时删除Lucene的对应索引
    public void deleteWeiboIndex(int weiboId) throws Exception;

    //发布或者转发微博时，同时增加Lucene的对应索引
    public void updateWeiboIndex(int weiboId, WeiboCustom weiboCustom) throws Exception;

}
