package com.knowledgex.web.view;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgex.domain.Tag;

@SuppressWarnings("serial")
public class TagBean implements Serializable {

    @SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TagBean.class);
    
    private Tag tag;

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}
	
	public void clear() {
        if (tag != null) {
        	tag.setId(null);
        	tag.setTagName("");
        }
    }
    
}