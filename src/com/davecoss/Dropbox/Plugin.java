package com.davecoss.Dropbox;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JDialog;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWriteMode;

import com.davecoss.java.plugin.PluginException;
import com.davecoss.java.plugin.PluginInitException;

public class Plugin implements com.davecoss.java.plugin.StoragePlugin {

	private DbxClient client = null;
	private final Collection<String> functionlist;
	private File jarfile = null;
	
	public Plugin() {
		client = null;
		functionlist = new ArrayList<String>();
	}

        @Override
        public void init(PrintStream output, InputStream input) throws PluginInitException {
		try {
		    client = Connector.connect(new APIKeyStore("appkey.properties"), output, input);
		} catch (Exception e) {
			throw new PluginInitException("Error creating Dropbox Client", e);
		}
	}

        @Override
        public void init(JDialog parent) throws PluginInitException {
		try {
		    client = Connector.connect(new APIKeyStore("appkey.properties"), parent);
		} catch (Exception e) {
			throw new PluginInitException("Error creating Dropbox Client", e);
		}
	}


	@Override
	public Collection<String> list_functions() throws PluginException {
		return functionlist;
	}

	@Override
	public void destroy() throws PluginException  {
		if(client != null)
			client = null;
	}
	
	@Override
    public URI mkdir(String path) {
	    try {
			DbxEntry.Folder retval = FileUtils.mkdir(path, client);
			return new URI("dbx:" + retval.path);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }

	@Override
	public boolean has_function(String function_name) throws PluginException {
		return functionlist != null && functionlist.contains(function_name);
	}

	@Override
	public File get_jarfile() {
		return jarfile;
	}
	
	@Override
	public File set_jarfile(File jarfile) {
		return (this.jarfile = jarfile);
	}

	@Override
	public String get_protocol() {
		return "dbx";
	}

	@Override
	public boolean isFile(URI uri) {
	    try {
		return client.getMetadata(uri.getPath()).isFile();
	    } catch(DbxException de) {
		return false;
	    }
	}

	@Override
	public boolean exists(URI uri) {
	    try {
		return client.getMetadata(uri.getPath()) != null;
	    } catch(DbxException de) {
		return false;
	    }
	}

	@Override
	public URI[] listFiles(URI uri) {
	    DbxEntry.WithChildren dirents = null;
	    try {
		dirents = client.getMetadataWithChildren(uri.getPath());
	    } catch(DbxException de) {
		return null;
	    }

	    if(dirents == null || dirents.children.size() == 0)
		return new URI[]{};
	    
	    URI[] retval = new URI[dirents.children.size()];
	    Iterator<DbxEntry> dirent = dirents.children.iterator();
	    int idx = 0;
	    while(dirent.hasNext()) {
		DbxEntry entry = dirent.next();
		try {
		    retval[idx++] = new URI("dbx:" + entry.path);
		} catch(URISyntaxException urise) {
		    return null;
		}
	    }
	    return retval;
	}

	@Override
	public URI saveStream(InputStream input, int amount_to_write,
			URI destination) throws PluginException {
		try {
			DbxEntry.File retval = FileUtils.upload_stream(input, amount_to_write, destination.getPath(), client);
			return new URI("dbx:" + retval.path);
    	} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public InputStream readStream(URI uri) throws PluginException {
		try {
			DbxClient.Downloader downloader = client.startGetFile(
					uri.getPath(), null);
			return new DropboxInputStream(downloader);
		} catch (DbxException de) {
			throw new PluginException("Error starting download.", de);
		}
	}

	@Override
	public OutputStream getOutputStream(URI uri) throws PluginException {
	    DbxClient.Uploader uploader = client.startUploadFileChunked(uri.getPath(), DbxWriteMode.force(), -1);
	    return new DropboxOutputStream(uploader);
	}

}