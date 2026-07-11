package com.iot.platform.common;
import lombok.Data;
import java.util.List;
@Data
public class PageResult<T> {
    private long pageNum; private long pageSize; private long total; private long pages; private List<T> list;
    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.setPageNum(pageNum); r.setPageSize(pageSize); r.setTotal(total);
        r.setPages((total + pageSize - 1) / pageSize); r.setList(list);
        return r;
    }
}
