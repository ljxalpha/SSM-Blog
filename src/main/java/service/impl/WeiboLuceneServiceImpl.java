package service.impl;

import mapper.dao.LuceneDao;
import org.springframework.beans.factory.annotation.Autowired;
import po.Page;
import po.WeiboCustom;
import service.WeiboLuceneService;
import service.WeiboService;

import java.util.*;

public class WeiboLuceneServiceImpl implements WeiboLuceneService {

    @Autowired
    LuceneDao luceneDao;

    @Autowired
    WeiboService weiboService;

    @Override
    public Page<WeiboCustom> queryWeiboByWordWithLucene(String keyWord, int pageNo) throws Exception {
        Page<WeiboCustom> page = new Page<>();
        page.setPageNo(pageNo);
        int pageSize = 10;
        page.setPageSize(pageSize);
        int start = (pageNo - 1) * pageSize;
        int rows = pageSize;
        Map<String, Object> luceneResMap = luceneDao.queryWeiboByWordWithLucene(keyWord, start, pageSize);
        Map<Integer, String> weiboIdAndContentMap = (Map<Integer, String>)luceneResMap.get("weiboIdAndContentMap");
        if(weiboIdAndContentMap.size() == 0){
            page.setResults(new ArrayList<WeiboCustom>());
            return page;
        }
        List<Integer> weiboIdList = new ArrayList<>(weiboIdAndContentMap.keySet());
        List<WeiboCustom> weiboCustomList = weiboService.queryWeiboByWeiboIdList(weiboIdList);
        for(WeiboCustom weiboCustom : weiboCustomList){
            weiboCustom.setContent(weiboIdAndContentMap.get(weiboCustom.getWeiboId()));
        }
        page.setResults(weiboCustomList);
        page.setTotalRecord((int)luceneResMap.get("totalHits"));
        return page;
    }

    @Override
    public void deleteWeiboIndex(int weiboId) throws Exception {
        luceneDao.deleteWeiboIndex(weiboId);
    }

    @Override
    public void updateWeiboIndex(int weiboId, WeiboCustom weiboCustom) throws Exception {
        luceneDao.updateWeiboIndex(weiboId, weiboCustom);
    }


}
