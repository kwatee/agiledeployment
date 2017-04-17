/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.descriptor;

import java.util.HashSet;
import java.util.Set;

import net.kwatee.agiledeployment.repository.dto.FileDto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DescriptorDifferences {

	private String error;
	private String errorDetails;
	final private HashSet<FileDto> changes;
	final private HashSet<FileDto> additions;
	final private HashSet<FileDto> removals;

	DescriptorDifferences() {
		this.error = StringUtils.EMPTY;
		this.errorDetails = StringUtils.EMPTY;
		this.changes = new HashSet<FileDto>(0);
		this.additions = new HashSet<FileDto>(0);
		this.removals = new HashSet<FileDto>(0);
	}

	public String getError() {
		return this.error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getErrorDetails() {
		return this.errorDetails;
	}

	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}

	public Set<FileDto> getChanges() {
		return this.changes;
	}

	public Set<FileDto> getAdditions() {
		return this.additions;
	}

	public Set<FileDto> getRemovals() {
		return this.removals;
	}

	public boolean hasDifferences() {
		return CollectionUtils.isNotEmpty(this.changes) || CollectionUtils.isNotEmpty(this.additions) || CollectionUtils.isNotEmpty(this.removals);
	}
}
