package com.lynxcat.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NightingaleNote {
	private String id;
	private String ident;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIdent() {
		return ident;
	}

	public void setIdent(String ident) {
		this.ident = ident;
	}

	public NightingaleNote(String id, String ident) {
		this.id = id;
		this.ident = ident;
	}

	public NightingaleNote() {
	}
}

