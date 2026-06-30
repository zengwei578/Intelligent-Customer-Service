package baize.code.java.service.impl;

import baize.code.java.service.GoodsDocumentService;
import baize.code.java.service.GoodsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import baize.code.java.entity.Goods;
import baize.code.java.entity.GoodsDocument;
import baize.code.java.mapper.GoodsDocumentMapper;
import baize.code.java.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static baize.code.java.code.DocumentCode.GOODS_ID;

@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {
    private final GoodsDocumentService goodsDocumentService;
    private final GoodsDocumentMapper goodsDocumentMapper;
    private final MilvusVectorStore vectorStore;

    @Override
    @Transactional
    public int add(Goods goods) {
        save(goods);
        return goods.getId();
    }


    /**
     * 这个修改的方法只对goods表中的树做修改
     * @param goods 修改的树
     * @return 修改结果
     */
    @Override
    public Boolean update(Goods goods) {
        return updateById(goods);
    }

    /**
     * 根据id查询商品
     * @param id 商品id
     * @return 商品信息和对应的文档数据
     */
    @Override
    public Goods detailById(Integer id) {
        Goods goods = getById(id);
        // 查询对应的文档
        List<GoodsDocument> documents = goodsDocumentMapper.selectList(new LambdaQueryWrapper<>(GoodsDocument.class)
                .eq(GoodsDocument::getGoodsId, id));
        goods.setDocuments(documents);
        return goods;
    }

    @Override
    public void delete(Integer goodsId) {
        //根据商品id删除商品文档
        int result = goodsDocumentMapper.delete(new LambdaQueryWrapper<GoodsDocument>()
                .eq(GoodsDocument::getGoodsId, goodsId));
        if(result<=0){
            throw new IllegalArgumentException("删除失败");
        }
        //根据商品id删除对应的向量数据
        vectorStore.delete(new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(GOODS_ID), new Filter.Value(goodsId)));

        removeById(goodsId);
    }
}