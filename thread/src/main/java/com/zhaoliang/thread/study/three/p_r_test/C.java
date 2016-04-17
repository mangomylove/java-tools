package com.zhaoliang.thread.study.three.p_r_test;

public class C {
	private String lock;

	public C(String lock){
		this.lock = lock;
	}

	public void getValue() {
		try {
			synchronized (lock) {
				if (ValueObject.value.equals("")) {
					lock.wait();
				}
				System.out.println("get = " + ValueObject.value);
				ValueObject.value = "";
				lock.notify();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
