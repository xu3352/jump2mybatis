package com.xu3352.ideaplugin.mybatis;

/**
 * Mybatis Mapper Config
 */
public class MapperConfig {
    private String namespace = "";
    private String keyword = "";

    public MapperConfig() {
    }

    /** 按选择的文本切分: namespace.keyword */
    public MapperConfig(String selectText) {
        if (selectText != null) {
            String[] dict = selectText.split("\\.");
            this.namespace = dict[0];
            this.keyword = dict.length < 2 ? "" : dict[1];
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return "MapperConfig{" +
                "namespace='" + namespace + '\'' +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
