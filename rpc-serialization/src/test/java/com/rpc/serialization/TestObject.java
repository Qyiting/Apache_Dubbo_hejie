package com.rpc.serialization;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 测试用的简单对象类
 */
public class TestObject {
    
    private Long id;
    private String name;
    private boolean active;
    private List<String> tags;
    private Map<String, Object> properties;
    
    // 默认构造函数（ProtoStuff需要）
    public TestObject() {
    }
    
    public TestObject(Long id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestObject that = (TestObject) o;
        return active == that.active &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(properties, that.properties);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, active, tags, properties);
    }
    
    @Override
    public String toString() {
        return "TestObject{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", tags=" + tags +
                ", properties=" + properties +
                '}';
    }
}