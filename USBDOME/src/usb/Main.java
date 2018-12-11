package usb;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbPort;
import javax.usb.UsbServices;

public class Main {
	final int  vendorId=0x00, productId=0x00;
	public static void main(String[] args) throws Exception  {
		// UsbHostManager.getUsbServices().getRootUsbHub()
		  UsbServices services = UsbHostManager.getUsbServices();
		  DumpDeviceTree tree=new DumpDeviceTree();
		  tree.dump(services.getRootUsbHub(), 5);
		  DumpNames dump=new DumpNames();
		  dump.processDevice(services.getRootUsbHub());
		  DumpDevices devices=new DumpDevices();
		  devices.getDumpDevices();
	}
	public  UsbDevice findDevice(UsbHub hub, short vendorId, short productId)
	{
	    for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
	    {
	        UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
	        if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
	        if (device.isUsbHub())
	        {
	            device = findDevice((UsbHub) device, vendorId, productId);
	            if (device != null) return device;
	        }
	    }
	    return null;
	}
}

class DumpDeviceTree
{
    /**
     * Dumps the specified device and its sub devices.
     * 
     * @param device
     *            The USB device to dump.
     * @param level
     *            The indentation level.
     */
    public   void dump(UsbDevice device, int level)
    {
        for (int i = 0; i < level; i += 1)
            System.out.print("  ");
        System.out.println(device);
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
            {
                dump(child, level + 1);
            }
        }
    }

}
class DumpNames
{
    /**
     * Dumps the name of the specified device to stdout.
     * 
     * @param device
     *            The USB device.
     * @throws UnsupportedEncodingException
     *             When string descriptor could not be parsed.
     * @throws UsbException
     *             When string descriptor could not be read.
     */
    private   void dumpName(final UsbDevice device)
        throws UnsupportedEncodingException, UsbException
    {
        // Read the string descriptor indices from the device descriptor.
        // If they are missing then ignore the device.
        final UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
        final byte iManufacturer = desc.iManufacturer();
        final byte iProduct = desc.iProduct();
        if (iManufacturer == 0 || iProduct == 0) return;

        // Dump the device name
        System.out.println(device.getString(iManufacturer) + " "
            + device.getString(iProduct));
    }

    /**
     * Processes the specified USB device.
     * 
     * @param device
     *            The USB device to process.
     */
    public   void processDevice(final UsbDevice device)
    {
        // When device is a hub then process all child devices
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
            {
                processDevice(child);
            }
        }

        // When device is not a hub then dump its name.
        else
        {
            try
            {
                dumpName(device);
            }
            catch (Exception e)
            {
                // On Linux this can fail because user has no write permission
                // on the USB device file. On Windows it can fail because
                // no libusb device driver is installed for the device
                System.err.println("Ignoring problematic device: " + e);
            }
        }
    }

    /**
     * Main method.
     * 
     * @param args
     *            Command-line arguments (Ignored)
     * @throws UsbException
     *             When an USB error was reported which wasn't handled by this
     *             program itself.
     */
    
}

class DumpDevices
{
    /**
     * Dumps the specified USB device to stdout.
     * 
     * @param device
     *            The USB device to dump.
     */
    private   void dumpDevice(final UsbDevice device)
    {
        // Dump information about the device itself
        System.out.println(device);
        final UsbPort port = device.getParentUsbPort();
        if (port != null)
        {
            System.out.println("Connected to port: " + port.getPortNumber());
            System.out.println("Parent: " + port.getUsbHub());
        }

        // Dump device descriptor
        System.out.println(device.getUsbDeviceDescriptor());

        // Process all configurations
        for (UsbConfiguration configuration: (List<UsbConfiguration>) device
            .getUsbConfigurations())
        {
            // Dump configuration descriptor
            System.out.println(configuration.getUsbConfigurationDescriptor());

            // Process all interfaces
            for (UsbInterface iface: (List<UsbInterface>) configuration
                .getUsbInterfaces())
            {
                // Dump the interface descriptor
                System.out.println(iface.getUsbInterfaceDescriptor());

                // Process all endpoints
                for (UsbEndpoint endpoint: (List<UsbEndpoint>) iface
                    .getUsbEndpoints())
                {
                    // Dump the endpoint descriptor
                    System.out.println(endpoint.getUsbEndpointDescriptor());
                }
            }
        }

        System.out.println();

        // Dump child devices if device is a hub
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
            {
                dumpDevice(child);
            }
        }
    }

    /**
     * Main method.
     * 
     * @param args
     *            Command-line arguments (Ignored)
     * @throws UsbException
     *             When an USB error was reported which wasn't handled by this
     *             program itself.
     */
    public   void getDumpDevices() throws UsbException
    {
        // Get the USB services and dump information about them
        final UsbServices services = UsbHostManager.getUsbServices();
        System.out.println("USB Service Implementation: "
            + services.getImpDescription());
        System.out.println("Implementation version: "
            + services.getImpVersion());
        System.out.println("Service API version: " + services.getApiVersion());
        System.out.println();

        // Dump the root USB hub
        dumpDevice(services.getRootUsbHub());
    }
}