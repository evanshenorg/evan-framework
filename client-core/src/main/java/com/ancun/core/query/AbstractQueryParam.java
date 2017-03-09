package com.ancun.core.query;

import java.io.Serializable;
import java.util.Arrays;

//@JsonInclude(Include.NON_NULL)
//@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractQueryParam<T> implements Serializable, QueryParam {
    private static final long serialVersionUID = 2941991018140190485L;

    private String sort; // 排序表达式
    private String sortCode; // 排序值
    private int pageNo; // 当前是第几页，从1开始记数
    private int pageSize; // 每页多少条
    private String[] columns;
    private boolean sortByDefault = true;
    private boolean includeDeleted;
    private T offset; // 列表最后一个排序值

    /**
     * 排序表达式
     */
    @Override
    public String getSort() {
        return sort;
    }

    /**
     * 排序表达式
     */
    public void setSort(String sort) {
        this.sort = sort;
    }

    /**
     * 当前页 从1开始
     */
    @Override
    public int getPageNo() {
        return pageNo;
    }

    /**
     * 当前页 从1开始
     */
    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
        if (pageNo < 1) {
            this.pageNo = 1;
        }
    }

    /**
     * 每页记录数
     */
    @Override
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 每页记录数
     */
    public void setPageSize(int pageSize) {
        if (this.pageNo == 0) {
            this.pageNo = 1;
        }
        this.pageSize = pageSize;
    }

    @Override
    //@JsonIgnore
    public int getStartRow() {
        return (pageNo - 1) * pageSize + 1;
    }

    @Override
    //@JsonIgnore
    public int getStartRowBegin0() {
        return (pageNo - 1) * pageSize;
    }

    @Override
    //@JsonIgnore
    public int getEndRow() {
        return pageNo * pageSize;
    }

    @Override
    public String[] getColumns() {
        return columns;
    }

    public void setColumns(String... columns) {
        this.columns = columns;
    }

    /**
     * 排序标示，不是真正的排序表达式，用于参数传入，然后转换成排序表达式
     */
    public String getSortCode() {
        return sortCode;
    }

    /**
     * 排序标示，不是真正的排序表达式，用于参数传入，然后转换成排序表达式
     */
    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public boolean isSortByDefault() {
        return sortByDefault;
    }

    public void setSortByDefault(boolean sortByDefault) {
        this.sortByDefault = sortByDefault;
    }

    public boolean isIncludeDeleted() {
        return includeDeleted;
    }

    public void setIncludeDeleted(boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
    }

    public T getOffset() {
        return offset;
    }

    public void setOffset(T offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "AbstractQueryParam{" +
                "sort='" + sort + '\'' +
                ", sortCode='" + sortCode + '\'' +
                ", pageNo=" + pageNo +
                ", pageSize=" + pageSize +
                ", columns=" + Arrays.toString(columns) +
                ", sortByDefault=" + sortByDefault +
                ", includeDeleted=" + includeDeleted +
                ", offset=" + offset +
                '}';
    }
}