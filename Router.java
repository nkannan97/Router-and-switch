package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}
	
	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }
	
	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}
	
	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
                etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                             */
		// check if the packet is an IPv4
		boolean flag;
		short header_version = etherPacket.getEtherType();
		// if the header version is IPv4 continue
		if(header_version == Ethernet.TYPE_IPv4) {

			// to  get the IPv4 header
			IPv4 header = (IPv4)etherPacket.getPayload();
			// get the checksum from the header
			short check_sum = header.getChecksum();
			// set the checksum of the packet to zero
			short var_check = header.setChecksum((short)0);
			// get the header length
			byte hlen = header.getHeaderLength();
			// serialize() -> not sure
			short header_length = (short)(hlen*4);
			
			if (header.getOptions() != null) {
				optionsLength = header.getOptions().length / 4;
				headerLength = (byte) (hlen + optionsLength);

			}

			byte[] data = new byte[header_length]
			
			ByteBuffer bb = ByteBuffer.wrap(data);
	
			totalLength = (short) pkt.getTotalLength();

			bb.put((byte) (((header.getVersion() & 0xf) << 4) | (hlen & 0xf)));

			bb.put(header.getDiffServ());

			System.out.println("Diff serv ==> " + header.getDiffServ());
			bb.putShort(totalLength);
			System.out.println("total length ==> " + header.getTotalLength());

			bb.putShort(header.getIdentification());
			System.out.println("ident==> " + header.getIdentification());

			bb.putShort((short) (((header.getFlags() & 0x7) << 13) | (header.getFragmentOffset() & 0x1fff)));

			bb.put(header.getTtl());
			System.out.println("ttl ==> "+header.getTtl());

			bb.put(header.getProtocol());
			System.out.println("protocol ==> "+header.getProtocol());

			bb.putShort(header.getChecksum());
			System.out.println("checksum ==> " + header.getChecksum());

			bb.putInt(header.getSourceAddress());
			System.out.println("src addrs ==> " + header.getSourceAddress());

			bb.putInt(header.getDestinationAddress());
			System.out.println("Destination addrs ==> " + header.getDestinationAddress());

			if (header.getOptions() != null){
				System.out.println("options ==> " + header.getOptions());

				bb.put(header.getOptions());
			}


			if (var_check == 0) {
            			bub.rewind();
            			int accumulation = 0;
                          	for (int i = 0; i < hlen * 2; ++i) {
                			accumulation += 0xffff & bb.getShort();
            			}
            			accumulation = ((accumulation >> 16) & 0xffff)
                    				+ (accumulation & 0xffff);

            			short new_checksum= (short) (~accumulation & 0xffff);

			
			        // check if the computed checksum is same as the original checksum
				if(var_check == check_sum) {
					header.setChecksum((short) (~accumulation & 0xffff));
					// decrement the ttl
					header.setTtl((byte)(header.getTtl() -1));
					// check for the ttl
					if(header.getTtl() > 0) {
	
						// decrement the TTL
						//header.setTtl(header.getTtl() -1);

						// get the Keyset()
						Set<String>interface_key_Set = getInterfaces().keySet();

						// iterate through the keySet()
						for(String interface_names: interface_key_Set) {
								// get the interface through its mapped interface name
								Iface in_face = interfaces.get(interface_names);

								// get the ip address corresponding to the interface
								int in_face_ip = in_face.getIpAddress(); 
								
								// if destIP doesnt match interface IP continue
								if(header.getDestinationAddress() != in_face_ip) {
										
									// using the lookup() in routeTable obtain routeEntry
									RouteEntry entry = routeTable.lookup(header.getDestinationAddress());
									// check for a match
									if(entry!=null) {
									
									int next_hop_ip = 0;
									// check if the gateway address exists--> not sure about the checking condition
									  if(entry.getGatewayAddress() == 0)	{		
										next_hop_ip = entry.getGatewayAddress();
										}
									   else{
										next_hop_ip = entry.getDestiantionAddress();
									   }
									  // lookup() the ARPCache to obtain the MACAddress for corresponding IP
									   ArpEntry cache_entry = arpcache.lookup(next_hop_ip);
									 // get the MACAdd
									   MACAddress entry_mac = cache_entry.getMac();

									 // this address is the new destination mac address for the ethernet frame 
									   byte[] dest_mac_address = entry_mac.toBytes();
									   byte[] new_source_mac_Address = etherPacket.getDestinationMACAddress();

									   etherPacket.setDestiantionMACAddress(dest_mac_address);

								        // setting the outgoing interfaces's MACAddress as the source MACADD
									   etherPacket.setSourceMACAddress(source_mac_address);	
								
								        // find the interface to which the packet has to be sent
									   Set<String> outgoing_interfaces = getInterfaces().keySet()

									   for(String interfaces_name: outgoing_interfaces) {

											 if(etherPacket.getDestinationMAC() == getInterfaces().get(interfaces_name).getMacAddress()) {
													Iface in_face = getInterfaces().get(interfaces_name);
													break();
											
												}
										} 
									       
										if(in_face!=null) {

											sendPacket(etherPacket,in_face);
										}
								  	
                                                                        }
									
										
								}

						}
					
					}
					

				}

			} 			
						 

		}		
		
		/********************************************************************/
	}
}
