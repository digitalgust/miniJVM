/* SPDX-License-Identifier: MIT */

package java.lang.reflect;

public interface GenericDeclaration extends AnnotatedElement {
  TypeVariable<?>[] getTypeParameters();
}
