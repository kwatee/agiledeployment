/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

public class IdNameDto {

	private Object id;
	private String name;

	public IdNameDto(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public IdNameDto(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public Object getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}
}
