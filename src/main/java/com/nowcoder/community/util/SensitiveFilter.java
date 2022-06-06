package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

        //前缀树
        private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

        //替换符
        private static final String REPLACEMENT = "***";

        //根节点
        private TrieNode rootNode=new TrieNode();

        @PostConstruct
        public void init(){
                try (
                        InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                ){
                    String keyword;
                    while((keyword = reader.readLine())!=null){
                        //添加到前缀树
                        this.addKeyword(keyword);
                    }
                }
                catch (IOException e){
                    logger.error("加载敏感词文件失败！"+e.getMessage());
                }
        }


    //将一个敏感词插入到前缀树中
    private void addKeyword(String keyword) {
        TrieNode node = rootNode;
        for (int i = 0; i < keyword.length() ; i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = node.getSubNode(c);
            //如果没有子节点 则创建一个
            if(subNode==null){
                subNode = new TrieNode();
                //将新建的节点跟当前节点连接起来
                node.addSubNode(c,subNode);
            }
            //指针移到子节点
            node=subNode;
        }
        node.setKeywordEnd(true);
    }


    /**
     * 过滤敏感词
     * @param text 待过滤的版本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        // 指针 1：前缀树的工作指针
        TrieNode tempNode = rootNode;
        // 指针 2：指向文本中某个敏感词的第一位
        int begin = 0;
        // 指针 3；指向文本中某个敏感词的最后一位
        int end = 0;

        // 记录过滤后的文本（结果）
        StringBuilder sb = new StringBuilder();

        while (end < text.length()) {
            char c = text.charAt(end);
            // 跳过符号（防止敏感词混合符号，比如 ☆赌☆博）
            if (isSymbol(c)) {
                // 若指针 1 处于根节点，则将此符号计入结果（直接忽略），让指针 2 向下走一步
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin ++;
                }
                // 无论符号在开头还是在中间，指针 3 都会向下走一步
                end ++;
                continue;
            }

            // 检查子节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 以指针 begin 开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一位的判断
                begin ++;
                end = begin;
                // 指针 1 重新指向根节点
                tempNode = rootNode;
            }
            else if (tempNode.isKeywordEnd()) {
                // 发现敏感词，将 begin~end 的字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一位的判断
                end ++;
                begin = end;
                // 指针 1 重新指向根节点
                tempNode = rootNode;
            }
            else {
                // 检查下一个字符
                end ++;
            }
        }

        // 将最后一批字符计入结果（如果最后一次循环的字符串不是敏感词，上述的循环逻辑不会将其加入最终结果）
        sb.append(text.substring(begin));

        return sb.toString();
    }

    private boolean isSymbol(Character c){
        //ox2E80~0x9FFF是 东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);

    }

    private class TrieNode{
        // 关键词结束标识
        private boolean isKeywordEnd =false;

        // 子节点(key是下级字符(可以理解成路径上的值)，value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd(){
            return isKeywordEnd;
        }

        public  void setKeywordEnd(boolean keywordEnd){
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c, node);
        }

        //获取子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }
}

