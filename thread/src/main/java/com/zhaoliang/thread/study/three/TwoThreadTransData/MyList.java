package com.zhaoliang.thread.study.three.TwoThreadTransData;

import java.util.ArrayList;
import java.util.List;

public class MyList {
	private List<String> list = new ArrayList<String>();

	public void add() {
		list.add("hello");
	}

	public int size() {
		return list.size();
	}
}
