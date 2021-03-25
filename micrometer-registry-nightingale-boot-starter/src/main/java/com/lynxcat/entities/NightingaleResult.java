package com.lynxcat.entities;

import com.fasterxml.jackson.annotation.JsonFormat;

public class NightingaleResult <T>{
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	public T dat;
	public String err;

	public T getDat() {
		return dat;
	}

	public void setDat(T dat) {
		this.dat = dat;
	}

	public String getErr() {
		return err;
	}

	public void setErr(String err) {
		this.err = err;
	}

	public NightingaleResult() {

	}

	public NightingaleResult(T dat, String err) {
		this.dat = dat;
		this.err = err;
	}
}
