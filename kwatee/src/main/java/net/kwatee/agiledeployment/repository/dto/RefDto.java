package net.kwatee.agiledeployment.repository.dto;

public class RefDto {

	private String ref;
	private Boolean canceled;

	public RefDto() {

	}

	public RefDto(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return this.ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public Boolean getCanceled() {
		return this.canceled;
	}

	public void setCanceled(Boolean canceled) {
		if (canceled != null && canceled.booleanValue())
			this.canceled = canceled;
		else
			this.canceled = null;
	}
}
