package com.andreavendrame.ldb4docker.myjlibbig;

import com.andreavendrame.ldb4docker.myjlibbig.util.NameGenerator;

/**
 * Describes a name identified entity and provides some name generation
 * methods. Name generation is thread safe but it is not guaranteed to be 
 * universally unique (in time and space). The same name can be generated by 
 * different machines and runs.
 */
public abstract class AbstractNamed implements Named {
	private final String name;

	/**
	 * Use an automatically generated name.
	 * 
	 * @see #generateName()
	 */
	protected AbstractNamed() {
		this(NameGenerator.DEFAULT.generate());
	}

	protected AbstractNamed(String name) {
		if (name == null || name.isEmpty()){
			throw new IllegalArgumentException("Name can not be empty.");
		}
		this.name = name;
	}
	
	protected AbstractNamed(String name, boolean isPrefix) {
		if(isPrefix){
			this.name = name + NameGenerator.DEFAULT.generate();
		}else{
			if (name == null || name.isEmpty()){
				throw new IllegalArgumentException("Name can not be empty.");
			}else{
				this.name = name;
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * Comparison is based on names also for inherited classes.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		AbstractNamed other = null;
		try {
			other = (AbstractNamed) obj;
		} catch (ClassCastException e) {
			return false;
		}
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
