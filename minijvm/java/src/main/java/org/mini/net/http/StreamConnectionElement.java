/*
 * @(#)StreamConnectionElement.java	1.10 02/09/09 @(#)
 *
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package org.mini.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import javax.cldc.io.StreamConnection;

/**
 * This class implements the necessary functionality
 * for an HTTP connection pool element container. Each
 * element contains appropriate http connection information
 * including a reference to the underlying socket stream connection.
 * 
 * @version 1.0
 */
public class StreamConnectionElement  
    implements StreamConnection {

    /** Current protocol. */
    String                      m_protocol;
    /** Current host name. */
    String                    m_host;
    /** Http port number. */
    int                       m_port;
    /** Connection stream to http server. */
    private StreamConnection          m_stream;
    /** Input stream to http server. */
    private DataInputStream             m_data_input_stream;
    /** Output stream to http server. */
    private DataOutputStream            m_data_output_stream;
    /** In use flag. */
    boolean                   m_in_use;
    /** Start time in milliseconds. */
    long                      m_time;
    /** Removed from pool flag while in use. (lingered too long) */
    boolean m_removed;
    
    /**
     * Create a new instance of this class.
     *
     * @param p_protocol protocol for the connection
     * @param p_host     hostname for the connection
     * @param p_port     port number for the connection
     * @param p_sc       stream connection
     * @param p_dos      data output stream from the stream connection
     * @param p_dis      data input stream from the stream connection
     */
    StreamConnectionElement(String p_protocol,
                            String p_host,
                            int p_port,
                            StreamConnection p_sc,
                            DataOutputStream p_dos,
                            DataInputStream p_dis) {
        m_protocol = p_protocol;
        m_host = p_host;
        m_port = p_port;
        m_stream = p_sc;
        m_data_output_stream = p_dos;
        m_data_input_stream = p_dis;
        m_time = System.currentTimeMillis();
    }

    /**
     * Clear the fields of the saved connection and release any 
     * system resources. Any open input and output streams are closed
     * as well as the connection itself.
     */
    public void close() {
	try {
	    if (m_data_output_stream != null) {
		m_data_output_stream.close();
		m_data_output_stream = null;
	    }
	    if (m_data_input_stream != null) {
		m_data_input_stream.close();
		m_data_input_stream = null;
	    }
	    if (m_stream != null) {
		m_stream.close();
		m_stream = null;
	    }
	} catch (IOException ioe) {
	    /*
	     * No special processing for errors, since the 
	     * the cached connection is no longer in use, and
	     * the server may already have shutdown its end
	     * of the connection.
	     */
	}
    }

    /**
     * Get the stream connection for this element.
     *
     * @return                     base stream connection
     */
    public StreamConnection getBaseConnection() {
        return m_stream;
    }

    /**
     * Get the current output stream for the stream connection.
     *
     * @return                     output stream
     */
    public OutputStream openOutputStream() {
        return m_data_output_stream;
    }

    /**
     * Get the current data output stream for the stream connection.
     *
     * @return                     data output stream
     */
    public DataOutputStream openDataOutputStream() { 
        return m_data_output_stream;
    }
    /**
     * Get the current data stream for the stream connection.
     *
     * @return                     input stream
     */
    public InputStream openInputStream() { 
        return m_data_input_stream;
    }
    /**
     * Get the current data input stream for the stream connection.
     *
     * @return                     data input stream
     */
    public DataInputStream openDataInputStream() { 
        return m_data_input_stream;
    }

}
                        
