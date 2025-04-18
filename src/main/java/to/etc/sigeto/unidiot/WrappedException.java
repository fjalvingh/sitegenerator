/*
 * DomUI Java User Interface - shared code
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.sigeto.unidiot;

import java.lang.reflect.InvocationTargetException;

/**
 * Helper class for managing the checked exception idiocy rampant
 * in Java code. It wraps checked exceptions in unchecked ones, and
 * has a method to unwrap them again at a higher level.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 25, 2010
 */
public class WrappedException extends RuntimeException {
	public WrappedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public WrappedException(final Throwable cause) {
		super(cause.toString(), cause);
	}

	static public RuntimeException wrap(final Throwable x) {
		if(x instanceof RuntimeException)
			return (RuntimeException) x;
		throw new WrappedException(x);
	}

	public static Exception unwrap(Exception x) {
		for(; ; ) {
			if(x instanceof WrappedException) {
				Throwable t = x.getCause();
				if(!(t instanceof Exception)) // Can we unwrap?
					return x; // No, keep wrapped
				x = (Exception) x.getCause();
			} else if(x instanceof InvocationTargetException) {
				Throwable t = x.getCause();
				if(!(t instanceof Exception)) // Can we unwrap?
					return x; // No, keep wrapped
				x = (Exception) x.getCause();
			} else
				return x;
		}
	}

	public static Exception unwrap(Throwable x) {
		if(x instanceof Exception)
			return unwrap((Exception) x);

		for(; ; ) {
			if(x instanceof WrappedException) {
				Throwable t = x.getCause();
				if(!(t instanceof Exception)) // Can we unwrap?
					break;
				x = x.getCause();
			} else if(x instanceof InvocationTargetException) {
				Throwable t = x.getCause();
				if(!(t instanceof Exception)) // Can we unwrap?
					break; // No, keep wrapped
				x = (Exception) x.getCause();
			} else
				break;
		}
		if(x instanceof Exception)
			return unwrap((Exception) x);
		return new WrappedException(x);
	}
}
