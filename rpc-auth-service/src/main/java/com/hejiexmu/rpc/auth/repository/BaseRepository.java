package com.hejiexmu.rpc.auth.repository;

import com.hejiexmu.rpc.auth.annotation.DataSource;
import com.hejiexmu.rpc.auth.config.DataSourceConfig;

import java.util.List;
import java.util.Optional;

/**
 * 基础Repository接口
 * 定义通用的数据访问方法
 * 
 * @author hejiexmu
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface BaseRepository<T, ID> {
    
    // ========== 写操作（使用主数据源） ==========
    
    /**
     * 保存实体
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    T save(T entity);
    
    /**
     * 批量保存实体
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    List<T> saveAll(List<T> entities);
    
    /**
     * 根据ID删除实体
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    boolean deleteById(ID id);
    
    /**
     * 删除实体
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    boolean delete(T entity);
    
    /**
     * 批量删除实体
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    int deleteAll(List<T> entities);
    
    /**
     * 根据ID列表批量删除
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    int deleteByIds(List<ID> ids);
    
    /**
     * 更新实体
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    boolean update(T entity);
    
    /**
     * 批量更新实体
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    int updateAll(List<T> entities);
    
    // ========== 读操作（使用从数据源） ==========
    
    /**
     * 根据ID查找实体
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    Optional<T> findById(ID id);
    
    /**
     * 根据ID列表查找实体
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    List<T> findByIds(List<ID> ids);
    
    /**
     * 查找所有实体
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    List<T> findAll();
    
    /**
     * 分页查找实体
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    PageResult<T> findAll(int page, int size);
    
    /**
     * 根据条件查找实体
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    List<T> findByCondition(T condition);
    
    /**
     * 根据条件分页查找实体
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    PageResult<T> findByCondition(T condition, int page, int size);
    
    /**
     * 统计总数
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    long count();
    
    /**
     * 根据条件统计数量
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    long countByCondition(T condition);
    
    /**
     * 检查实体是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    boolean existsById(ID id);
    
    /**
     * 根据条件检查实体是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    boolean existsByCondition(T condition);
    
    /**
     * 分页结果类
     */
    class PageResult<T> {
        private List<T> content;
        private long totalElements;
        private int totalPages;
        private int page;
        private int size;
        private boolean hasNext;
        private boolean hasPrevious;
        
        public PageResult() {
        }
        
        public PageResult(List<T> content, long totalElements, int page, int size) {
            this.content = content;
            this.totalElements = totalElements;
            this.page = page;
            this.size = size;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
        }
        
        // Getters and Setters
        public List<T> getContent() {
            return content;
        }
        
        public void setContent(List<T> content) {
            this.content = content;
        }
        
        public long getTotalElements() {
            return totalElements;
        }
        
        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
        
        public int getPage() {
            return page;
        }
        
        public void setPage(int page) {
            this.page = page;
        }
        
        public int getSize() {
            return size;
        }
        
        public void setSize(int size) {
            this.size = size;
        }
        
        public boolean isHasNext() {
            return hasNext;
        }
        
        public void setHasNext(boolean hasNext) {
            this.hasNext = hasNext;
        }
        
        public boolean isHasPrevious() {
            return hasPrevious;
        }
        
        public void setHasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
        }
    }
}