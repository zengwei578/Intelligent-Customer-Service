package baize.code.java.service.impl;

import baize.code.java.code.ResultCode;
import baize.code.java.common.Result;
import baize.code.java.service.GoodsDocumentService;
import baize.code.java.utils.documentload.DocumentHandler;
import com.alibaba.cloud.ai.transformer.splitter.RecursiveCharacterTextSplitter;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import baize.code.java.entity.GoodsDocument;
import baize.code.java.mapper.GoodsDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static baize.code.java.code.DocumentCode.FILE_ID;
import static baize.code.java.code.DocumentCode.GOODS_ID;
import static baize.code.java.code.ResultCode.SUCCESS;

@Service
@RequiredArgsConstructor
public class GoodsDocumentServiceImpl extends ServiceImpl<GoodsDocumentMapper, GoodsDocument> implements GoodsDocumentService {

    private final MilvusVectorStore vectorStore;

    private final DocumentHandler documentHandler;

    private  final RecursiveCharacterTextSplitter textSplitter;

    @Override
    public List<GoodsDocument> getListByGoodsId(Integer id) {
        return lambdaQuery().eq(GoodsDocument::getGoodsId, id).list();
    }



    @Override
    public Result<?> upload(MultipartFile file, Integer goodsId) {
        List<Document> documents = documentHandler.handleDocument(file);

        documents = textSplitter.split(documents);

        GoodsDocument goodsDocument = GoodsDocument.builder().goodsId(goodsId).name(file.getOriginalFilename()).build();
        if(!save(goodsDocument)){
            return Result.error(ResultCode.ADD_ERROR);
        }
        for (Document document : documents) {
            document.mutate().metadata(FILE_ID, goodsDocument.getId()).metadata(GOODS_ID, goodsId);
        }
        int batchSize = 10;
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
            vectorStore.add(batch);
        }
        return Result.success(SUCCESS,documents);
    }

    @Override
    public void delete(Integer id){
        if (!removeById(id)) {
            throw new RuntimeException("Delete failed");
        }
        vectorStore.delete(new Filter.Expression(Filter.ExpressionType.EQ,new Filter.Key(FILE_ID),new Filter.Value(id)));
    }


}
