package utils;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import po.WeiboCustom;

/*
 * GoodInfo的转换类
 * */
public class WeiboToDocUtils {

    /*
     * 将GoodDetails 转换成document 将GoodDetails 的值设置到document里面去...
     */
    public static Document WeiboCustomToDocument(WeiboCustom weiboCustom) {

        Document document = new Document();

        StringField idField = new StringField("weiboId", weiboCustom.getWeiboId().toString(), Store.YES);

        TextField contentField = new TextField("weiboContent", weiboCustom.getContent(),Store.YES);
        document.add(idField);
        document.add(contentField);

        return document;
    }
}
