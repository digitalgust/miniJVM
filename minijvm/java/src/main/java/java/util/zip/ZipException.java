/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools and Templates, and edit the template in the editor.
 */
package java.util.zip;

import java.io.IOException;

public class ZipException extends IOException {

    public ZipException() {
        super();
    }

    public ZipException(String message) {
        super(message);
    }
}
