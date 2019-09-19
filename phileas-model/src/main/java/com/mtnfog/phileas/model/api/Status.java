package com.mtnfog.phileas.model.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A status of Philter.
 */
public final class Status {

	private final String status;
	
	public Status(String status) {
		this.status = status;
	}

	@Override
	public int hashCode() {

		return new HashCodeBuilder(17, 37).
				append(status).
				toHashCode();

	}

	@Override
	public boolean equals(Object o) {

		return EqualsBuilder.reflectionEquals(this, o);

	}

	public String getStatus() {
		return status;
	}
	
}
