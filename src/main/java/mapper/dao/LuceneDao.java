package mapper.dao;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import po.Page;
import po.WeiboCustom;
import service.WeiboService;
import utils.Constant;
import utils.LuceneUtils;
import utils.SynonymAnalyzerUtil;
import utils.WeiboToDocUtils;

/*
 * 使用Lucene来操作索引库
 * */

public class LuceneDao {

    WeiboService weiboService;

    @Autowired
    public LuceneDao(WeiboService weiboService) throws Exception {
        this.weiboService = weiboService;
        createIndexForAll();
    }

	/*
	 * 增删改索引都是通过indexWriter对象完成
	 *
	 * */

    /*
     * 为所有微博建立索引
     * */
    public void createIndexForAll() throws Exception {
        System.out.println("正在给所有微博创建索引");
        IndexWriter indexWriter = LuceneUtils.getIndexWriterOfSP();
        indexWriter.commit();
        Term term = null;
        Document doc = null;
        Page<WeiboCustom> page = null;
        int pageNo = 1;
        do{
            page = weiboService.queryAllWeibo(pageNo);
            List<WeiboCustom> weiboCustomList = page.getResults();
            for(WeiboCustom weiboCustom : weiboCustomList){
                term = new Term("weiboId", weiboCustom.getWeiboId().toString());
                doc = WeiboToDocUtils.WeiboCustomToDocument(weiboCustom);
                indexWriter.updateDocument(term, doc);
//                indexWriter.addDocument(doc);
            }
            indexWriter.forceMerge(10);
            pageNo++;
        }while(pageNo <= page.getTotalPage());
        indexWriter.close();
        System.out.println("成功给所有微博创建索引");
    }


    /*
     * 为单个item建立索引
     * */
//    public void addIndex(WeiboCustom weiboCustom) throws IOException {
//
//        IndexWriter indexWriter = LuceneUtils.getIndexWriterOfSP();
//        Document doc = WeiboToDocUtils.WeiboCustomToDocument(weiboCustom);
//        indexWriter.addDocument(doc);
////        		indexWriter.forceMerge(10);//合并cfs文件。比如设定1，就是自动合并成一个索引cfs文件
//        indexWriter.close();
//    }


    /*
     * 删除索引，根据字段对应的值进行删除
     * */
    public void deleteWeiboIndex(Integer weiboId) throws IOException {

        IndexWriter indexWriter = LuceneUtils.getIndexWriterOfSP();
        //term!!!
        Term term = new Term("weiboId", weiboId.toString());
        //根据字段对应的值删除索引
        indexWriter.deleteDocuments(term);
        indexWriter.commit();
        indexWriter.close();
    }

    public void deleteAllIndex() throws IOException {
        System.out.println("将要删除Lucene的所有索引");
        IndexWriter indexWriter = LuceneUtils.getIndexWriterOfSP();
        indexWriter.deleteAll();
        indexWriter.commit();
        indexWriter.close();
        System.out.println("完成对Lucene的所有索引的删除");
    }

    /*
     * 先删除符合条件的记录，再创建一个符合条件的记录
     * */
    public void updateWeiboIndex(Integer weiboId,WeiboCustom weiboCustom) throws IOException {

        IndexWriter indexWriter = LuceneUtils.getIndexWriterOfSP();

        Term term = new Term("weiboId",weiboId.toString());

        Document document = WeiboToDocUtils.WeiboCustomToDocument(weiboCustom);

		/*
		 * 1.设置更新的条件
		 * 2.设置更新的内容的对象
		 * */
        indexWriter.updateDocument(term, document);

        indexWriter.close();
    }

    /*
	 * 分页查询
	 * */
    public Map<String, Object> queryWeiboByWordWithLucene(String keywords, int start, int rows) throws Exception {

        Map<String, Object> resMap = new HashMap<>();

        Directory directory = FSDirectory.open(new File(Constant.INDEXURL_ALL));//索引创建在硬盘上。
        IndexReader indexReader = LuceneUtils.getIndexReaderOfSP();
        IndexSearcher indexSearcher =  LuceneUtils.getIndexSearcherOfSP(indexReader);

        String result = SynonymAnalyzerUtil.analyzeChinese(keywords, true);
        //需要根据哪几个字段进行检索...
        String fields[] = {"weiboContent"};

        //查询分析程序（查询解析）
        QueryParser queryParser = new MultiFieldQueryParser(LuceneUtils.getMatchVersion(), fields, LuceneUtils.getAnalyzer());

        //不同的规则构造不同的子类...
        //title:keywords content:keywords
        Query query = queryParser.parse(result);

        //这里检索的是索引目录,会把整个索引目录都读取一遍
        //根据query查询，返回前N条
        TopDocs topDocs = indexSearcher.search(query, start+rows);
        resMap.put("totalHits", topDocs.totalHits);

        System.out.println("总记录数="+topDocs.totalHits);

        ScoreDoc scoreDoc[] = topDocs.scoreDocs;

        /**添加设置文字高亮begin*/
        //htmly页面高亮显示的格式化，默认是<b></b>即加粗
        Formatter formatter = new SimpleHTMLFormatter("<font color='red'>", "</font>");
        Scorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, scorer);

        //设置文字摘要（高亮的部分），此时摘要大小为10
        //int fragmentSize = 10;
        Fragmenter fragmenter = new SimpleFragmenter();
        highlighter.setTextFragmenter(fragmenter);

        /**添加设置文字高亮end*/
        Map<Integer, String> weiboIdAndContentMap = new HashMap<>();
        //防止数组溢出
        int endResult = Math.min(scoreDoc.length, start+rows);
        WeiboCustom weiboCustom = null;

        for(int i = start;i < endResult ;i++ ){
            weiboCustom = new WeiboCustom();
            //docID lucene的索引库里面有很多的document，lucene为每个document定义了一个编号，唯一标识，自增长
            int docID = scoreDoc[i].doc;
            System.out.println("标识docID="+docID);
            Document document = indexSearcher.doc(docID);
            /**获取文字高亮的信息begin*/
            System.out.println("==========================");
            TokenStream tokenStream = LuceneUtils.getAnalyzer().tokenStream("weiboContent", new StringReader(document.get("weiboContent")));
            String weiboContent = highlighter.getBestFragment(tokenStream, document.get("weiboContent"));
            System.out.println("weiboContent = "+weiboContent);
            System.out.println("==========================");
            /**获取文字高亮的信息end*/
            weiboIdAndContentMap.put(Integer.parseInt(document.get("weiboId")), weiboContent);
        }
        indexReader.close();
        resMap.put("weiboIdAndContentMap", weiboIdAndContentMap);
        return resMap;
    }

}
