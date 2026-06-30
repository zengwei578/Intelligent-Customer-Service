package baize.code.java.controller;

import baize.code.java.code.ResultCode;
import baize.code.java.common.Result;
import baize.code.java.service.GoodsDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/goodsDocument")
@RequiredArgsConstructor
public class GoodsDocumentController {
    private final GoodsDocumentService goodsDocumentService;

    /**
     * TODO:001文档上传
     * @param file 文档
     * @param goodsId 商品id
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<?> upload(@RequestBody MultipartFile file, Integer goodsId) {
        return goodsDocumentService.upload(file, goodsId);
    }

    /**
     * TODO:002 删除文档
     * @param id 文档id
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam Integer id) {
        goodsDocumentService.delete(id);
        return Result.success(ResultCode.SUCCESS);
    }
}
