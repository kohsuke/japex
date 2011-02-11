/**
 * 
 */
package japex.namedclasspaths;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import com.sun.japex.Japex;

/**
 * @author benson
 *
 */
public class Embedder {

	/**
	 * @param args
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException {
		String configFilePath = args[0];
		String driverClassDir = args[1];
		Japex japex = new Japex();
		ClassLoader driverLoader = new URLClassLoader(new URL[] {
				new File(driverClassDir).toURI().toURL(),
		}, Embedder.class.getClassLoader());
		japex.getNamedClasspaths().put("embedded", driverLoader);
		japex.run(configFilePath);
	}
}
