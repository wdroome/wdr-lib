package com.wdroome.altodata;

import java.security.MessageDigest;
import java.util.*;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.AltoResp_NetworkMap;
import com.wdroome.util.FileStringIterator;
import com.wdroome.util.IterableWrapper;
import com.wdroome.util.IteratorWithPosition;
import com.wdroome.util.StringUtils;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.CIDRSet;
import com.wdroome.util.inet.NamedCIDRSet;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.inet.UnknownAddressTypeException;

/**
 * A class that represents an ALTO network map.
 * This class maps PIDs to a set of CIDRs,
 * and maps internet addresses to PIDs.
 * @author wdr
 */
public class NetworkMap
{
	public static final String MAP_FILE_VTAG_PREFIX = "vtag=";
	public static final String MAP_FILE_MAP_ID_PREFIX = "map-id=";
	
	/**
	 * A frozen map with one PID, named "DEFAULT",
 	 * which covers all possible endpoints.
	 */
	public static final NetworkMap DUMMY_MAP;
	
	static {
		DUMMY_MAP = new NetworkMap("DUMMY");
		try {
			DUMMY_MAP.addCIDRs("DEFAULT", "0.0.0.0/0 ::/0");
		} catch (UnknownHostException e) {
		}
		DUMMY_MAP.freeze();
	}
		
	// Map PID => list of CIDRs in that PID.
	// The update methods ensure that m_pid2cidrs and m_cidr2pid are consistent.
	// When the NetworkMap is frozen, these maps are immutable.
	private final Map<String,CIDRSet> m_pid2cidrs
							= new HashMap<String,CIDRSet>();
	
	// Map CIDR => PID for that CIDR.
	// Because it's a TreeMap, and because CIDRAddresses
	// compare with longest-mask-first, iterating through this map
	// presents longest-masks first.
	// The update methods ensure that m_pid2cidrs and m_cidr2pid are consistent.
	// When the NetworkMap is frozen, these maps are immutable.
	private final TreeMap<CIDRAddress,String> m_cidr2pid
							= new TreeMap<CIDRAddress,String>();
	
	private boolean m_frozen = false;
	private final String m_mapId;
	private String m_vtag = null;
	private long m_lastModTime = 0;
	
	/**
	 * When the map is frozen, this becomes a sorted array of pid names.
	 */
	private String[] m_pidNames = null;
	
	/**
	 * When the map is frozen, this represents the "parent pid" relationship.
	 * That is, if all of pid P2's CIDRs are completely contained within the CIDRs of pid P1,
	 * then key "P2" has value "P1". Note that this relation exists only
	 * when there is a single, unambiguous containing PID.
	 * That is, P2 maps to P1 iff every PID Px that completely contains P2
	 * also completely contains P1. If there is no such unique pid P1,
	 * then P2 maps to null.
	 */
	private Map<String,String> m_containingPid = null;
	
	/**
	 * Create a new, empty Network Map.
	 * @param mapId Resource ID of this map.
	 */
	public NetworkMap(String mapId)
	{
		m_mapId = mapId;
	}
	
	/**
	 * Create a new map from the map defined by a network-map response.
	 * This c'tor quietly ignores CIDRs with unknown address types.
	 * The c'tor freezes the new map with the vtag given in resp.
	 * @param resp A NetworkMap response from an ALTO server.
	 * @throws UnknownHostException If a cidr isn't valid.
	 * @throws IllegalPidNameException If a PID name isn't legal.
	 * @throws IllegalVersionTagException If a version tag isn't legal.
	 */
	public NetworkMap(AltoResp_NetworkMap resp)
			throws  UnknownHostException,
					IllegalPidNameException, IllegalVersionTagException
	{
		for (String pid:resp) {
			validatePidName(pid);
			Iterator<String> iter = resp.getAddressTypes(pid);
			while (iter.hasNext()) {
				String addrType = iter.next();
				if (EndpointAddress.isKnownAddressType(addrType)) {
					try {
						addCIDRs(pid, resp.getCIDRs(pid, addrType), addrType);
					} catch (UnknownAddressTypeException e) {
						// Ignore unknown address types in response messages
					} catch (JSONException e) {
						// Ignore -- shouldn't happen
					}
				}
			}
			// Make sure we create the PID name, even if it has no CIDRs.
			getCIDRs(pid, true);
		}
		m_mapId = resp.getThisResourceId();
		String vtag = resp.getThisTag();
		if (vtag != null && !vtag.isEmpty())
			validateVersionTag(vtag);
		freeze(vtag, null);
	}

	/**
	 * Return true if the NetworkMap is frozen. That is, no updates are allowed.
	 */
	public boolean isFrozen()
	{
		return m_frozen;
	}
	
	/**
	 * Return the resource id for this network map.
	 */
	public String getMapId()
	{
		return m_mapId;
	}
	
	/**
	 * Return the version tag for a frozen map.
	 * @return The version tag for this map.
	 *		If the map isn't frozen, return null.
	 */
	public String getVtag()
	{
		return m_vtag;
	}
	
	/**
	 * Set the vtag. The map must not be frozen.
	 * @param vtag The vtag.
	 */
	public void setVtag(String vtag)
	{
		if (isFrozen())
			throw new IllegalStateException("Cannot update a frozen NetworkMap");
		m_vtag = vtag;
	}
	
	/**
	 * Return the last modification time of this map, as a system time stamp.
	 * This is the time when the network map was frozen.
	 * @return The freeze time of the map.
	 */
	public long getLastModTime()
	{
		return m_lastModTime;
	}
	
	/**
	 * Freeze this NetworkMap and set the vtag and PID name array.
	 * This also freezes all CIDR sets.
	 * @param vtag The version tag for this map (if not already set).
	 * 			We assume the caller has verified that vtag
	 * 			follows the ALTO RFC's rules for legal version tag strings.
	 * 			If vtag is null or blank, make one up.
	 * @param makeVtagPrefix If we make up a vtag, use this as prefix.
	 * @throws IllegalStateException If this map is already frozen.
	 */
	public synchronized void freeze(String vtag, String makeVtagPrefix)
	{
		if (isFrozen())
			throw new IllegalStateException("Cannot re-freeze a NetworkMap");
		if (m_vtag == null || m_vtag.equals("")) {
			if (vtag != null && !vtag.equals(""))
				m_vtag = vtag;
			else
				m_vtag = makeVtag(makeVtagPrefix);
		}
		ArrayList<String> pidNames = new ArrayList<String>();
		for (Map.Entry<String,CIDRSet> entry:m_pid2cidrs.entrySet()) {
			entry.getValue().freeze();
			pidNames.add(entry.getKey());
		}
		m_pidNames = pidNames.toArray(new String[pidNames.size()]);
		Arrays.sort(m_pidNames);
		calcContainingPids0();
		m_frozen = true;
		m_lastModTime = System.currentTimeMillis();
	}
	
	/**
	 * Freeze this network map and set the vtag to a random value.
	 * Quietly ignored if the map is already frozen.
	 */
	public void freeze()
	{
		if (!isFrozen()) {
			freeze(null, null);
		}
	}
	
	/**
	 * Create the m_containingPid map.
	 * Called when map is being frozen.
	 * <p>The immediate parent of CIDR C is the longest CIDR C' in the map that covers C.</p>
	 * <p>The immediate parent of PID P is the one PID
	 * that has the immediate parent CIDRs of every CIDR in P.
	 * If the parent CIDRs of P are in more than one PID,
	 * or if a CIDR in P does not have an immediate parent,
	 * then P does not have an immediate parent PID.
	 */
	private void calcContainingPids0()
	{
		m_containingPid = new HashMap<String, String>();
		
		// Create cidr2parent such that it maps each CIDR to the longest CIDR that covers it.
		// CIDRs that aren't covered by another CIDR are not in the map.
		Map<CIDRAddress, CIDRAddress> cidr2parent = new HashMap<CIDRAddress, CIDRAddress>();
		for (CIDRAddress cidr: m_cidr2pid.keySet()) {
			CIDRAddress longestParent = null;
			for (CIDRAddress xcidr: m_cidr2pid.keySet()) {
				if (xcidr != cidr && xcidr.covers(cidr)) {
					if (longestParent == null || xcidr.getMaskLen() > cidr.getMaskLen()) {
						longestParent = xcidr;
					}
				}
			}
			if (longestParent != null) {
				cidr2parent.put(cidr, longestParent);
			}
		}
		
		// For all PIDs:
		for (Map.Entry<String,CIDRSet> ent: m_pid2cidrs.entrySet()) {
			String pid = ent.getKey();
			CIDRSet cidrs = ent.getValue();
			String parent = null;

			// For all cidrs in pid:
			for (CIDRAddress cidr: cidrs.getCIDRs()) {
				// Get parent CIDR. If none, give up.
				CIDRAddress parCIDR = cidr2parent.get(cidr);
				if (parCIDR == null) {
					parent = null;
					break;
				}
				// Get PID with the parent CIDR.
				String pidWithCIDR = m_cidr2pid.get(parCIDR);
				if (pidWithCIDR == null) {
					// shouldn't happen
					parent = null;
					break;
				}
				// First time we find a parent PID, save it.
				// If we ever find another parent PID, give up.
				if (parent == null) {
					parent = pidWithCIDR;
				} else if (!parent.equals(pidWithCIDR)) {
					parent = null;
					break;
				}
			}
			
			// Save the parent PID, if any.
			if (parent != null) {
				m_containingPid.put(pid, parent);
			}
		}
	}
	
	/**
	 * Create the m_containingPid map.
	 * Called when map is being frozen.
	 * @see #calcContainingPids2()
	 */
	@SuppressWarnings("unused")
	private void calcContainingPids1()
	{
		m_containingPid = new HashMap<String, String>();
		
		// Create cidr2parent such that it maps each CIDR to the longest CIDR that covers it.
		// CIDRs that aren't covered by another CIDR are not in the map.
		Map<CIDRAddress, CIDRAddress> cidr2parent = new HashMap<CIDRAddress, CIDRAddress>();
		for (CIDRAddress cidr: m_cidr2pid.keySet()) {
			CIDRAddress longestParent = null;
			for (CIDRAddress xcidr: m_cidr2pid.keySet()) {
				if (xcidr != cidr && xcidr.covers(cidr)) {
					if (longestParent == null || xcidr.getMaskLen() > cidr.getMaskLen()) {
						longestParent = xcidr;
					}
				}
			}
			if (longestParent != null) {
				cidr2parent.put(cidr, longestParent);
			}
		}
		
		// Now for all PIDs and their CIDRSets ....
		for (Map.Entry<String,CIDRSet> ent: m_pid2cidrs.entrySet()) {
			String pid = ent.getKey();
			CIDRSet cidrs = ent.getValue();
			
			// Create "enclosingPids" with the names of the PIDs
			// with the immediate parent CIDRs of each of pid's CIDRs.
			Set<String> enclosingPids = new HashSet<String>();
			for (CIDRAddress cidr: cidrs.getCIDRs()) {
				CIDRAddress cidrParent = cidr2parent.get(cidr);
				if (cidrParent != null) {
					enclosingPids.add(m_cidr2pid.get(cidrParent));
				}
			}
			
			// Create "parents" with the cloned CIDRsets
			// for each pid that (at least partially) encloses "pid".
			Map<String, CIDRSet> parents = new HashMap<String, CIDRSet>();
			for (String xpid: enclosingPids) {
				CIDRSet xcidrs = new CIDRSet(m_pid2cidrs.get(xpid));
				xcidrs.freeze(false);
				parents.put(xpid, xcidrs);
			}
			
			// The parent of "pid" is the one pid in "parents"
			// that fully covers "pid" and is contained in all other parents.
			// If there is more than one, "pid" does not have a parent.
			String immediateParent = null;
			for (Map.Entry<String, CIDRSet> xent: parents.entrySet()) {
				String xpid = xent.getKey();
				CIDRSet xcidrs = xent.getValue();
				if (!xcidrs.fullyCovers(cidrs)) {
					continue;
				}
				boolean inAll = true;
				for (Map.Entry<String, CIDRSet> yent: parents.entrySet()) {
					String ypid = yent.getKey();
					if (!xpid.equals(ypid) && !yent.getValue().fullyCovers(xent.getValue())) {
						inAll = false;
						break;
					}
				}
				if (inAll) {
					if (immediateParent == null) {
						immediateParent = xpid;
					} else {
						immediateParent = null;
						break;
					}
				}
			}
			if (immediateParent != null) {
				m_containingPid.put(pid, immediateParent);
			}
		}
	}
	
	/**
	 * Alternate method of creating the m_containingPid map.
	 * Called when map is being frozen. For this algorithm,
	 * <p>PID P partially covers PID P iff some CIDR in P is a refinement of a CIDR in P.</p>
	 * <p>PID P fully covers PID P iff every CIDR in P is a refinement of a CIDR in P.</p>
	 * <p>The immediate parent of P is the PID P' such that P' fully covers P,
	 * and every P'' that partially covers P fully covers P'.
	 * If there is no such PID P', then P does not have an immediate parent.</p>
	 * @see #calcContainingPids1()
	 */
	@SuppressWarnings("unused")
	private void calcContainingPids2()
	{
		m_containingPid = new HashMap<String, String>();
		
		// Find the immediate parent (if any) for each PID p.
		for (Map.Entry<String,CIDRSet> ent: m_pid2cidrs.entrySet()) {
			String p = ent.getKey();
			CIDRSet pCIDRs = ent.getValue();
			
			// pc has all PIDs that partially cover p.
			Set<String> pc = new HashSet<String>();
			for (Map.Entry<String,CIDRSet> ent2: m_pid2cidrs.entrySet()) {
				if (ent2.getValue().partiallyCovers(pCIDRs)) {
					pc.add(ent2.getKey());
				}
			}
			// System.out.println("XXX: " + p + ": partially covered by " + pc);
			
			// Set immedParent to the unique PID in pc that fully covers p,
			// and is fully covered by all other PIDs in pc.
			String immedParent = null;
			for (String pp: pc) {
				CIDRSet ppCIDRs = m_pid2cidrs.get(pp);
				if (!ppCIDRs.fullyCovers(pCIDRs)) {
					continue;
				}
				// System.out.println("XXX: " + p + ": fully covered by " + pp);
				boolean coveredByAll = true;
				for (String pp2: pc) {
					if (!pp2.equals(pp) && !(m_pid2cidrs.get(pp2).fullyCovers(ppCIDRs))) {
						coveredByAll = false;
						break;
					}
				}
				if (coveredByAll) {
					// System.out.println("XXX: " + p + ": " + pp + " fully covered by all");
					if (immedParent == null) {
						immedParent = pp;
					} else {
						immedParent = null;
						break;
					}
				}
			}
			
			// Save p's immediate parent, if any, in m_containingPid.
			if (immedParent != null) {
				// System.out.println("XXX: " + p + ": immediate parent: " + immedParent);
				m_containingPid.put(p, immedParent);
			}
		}
	}
		
	/**
	 * If a PID is completely contained within another PID,
	 * return the name of that PID.  If a PID is contained
	 * within several PIDs, return the one PID that is enclosed
	 * by all the other containing PIDs.
	 * @param pid The name of a PID.
	 * @return The name of the smallest PID that contains "pid".
	 * 		If no PID contains "pid", or if "pid" is invalid,
	 * 		return null.
	 * @throws IllegalStateException If the map isn't frozen.
	 */
	public String getContainingPid(String pid)
	{
		if (!isFrozen())
			throw new IllegalStateException("NetworkMap.getContainingPid(): Only legal after map is frozen.");
		if (m_containingPid != null)
			return m_containingPid.get(pid);
		else
			return null;
	}
	
	/**
	 * Set or change the PID for a CIDR.
	 * Note this method does not verify that the PID name is legal;
	 * the caller must do that.
	 * @param pid The new PID.
	 * @param cidr The CIDR.
	 * @return The PID previously associated with this CIDR, or null.
	 * @throws IllegalStateException If this map is frozen.
	 */
	public synchronized String addCIDR(String pid, CIDRAddress cidr)
	{
		if (isFrozen())
			throw new IllegalStateException("Cannot update a frozen NetworkMap");
		String oldPid = m_cidr2pid.put(cidr, pid);
		if (oldPid != null) {
			if (oldPid.equals(pid))
				return oldPid;
			CIDRSet oldCidrs = getCIDRs(pid, false);
			if (oldCidrs != null)
				oldCidrs.remove(cidr);
		}
		CIDRSet cidrs = getCIDRs(pid, true);
		cidrs.add(cidr);
		return oldPid;
	}
	
	/**
	 * Set or change the pid for a set of CIDRs.
	 * Note this method does not verify that the PID name is legal;
	 * the caller must do that.
	 * @param pid The new pid.
	 * @param cidrSpecs An array of CIDR strings.
	 * @param addrType The default address type for those CIDRs.
	 * @throws IllegalStateException If this map is frozen.
	 * @throws UnknownAddressTypeException
	 *		If any of the source strings have an unknown address type prefix,
	 *		or if addrType is an unrecognized address type,
	 * @throws UnknownHostException
	 * 		If any of the source strings are not valid CIDR addresses.
	 */
	public synchronized void addCIDRs(String pid, String[] cidrSpecs, String addrType)
			throws UnknownHostException
	{
		if (cidrSpecs == null)
			return;
		if (isFrozen())
			throw new IllegalStateException("Cannot update a frozen NetworkMap");
		CIDRSet cidrs = getCIDRs(pid, true);
		for (String s: cidrSpecs) {
			if (s == null || s.equals(""))
				continue;
			CIDRAddress cidr = new CIDRAddress(s, addrType);
			String oldPid = m_cidr2pid.put(cidr, pid);
			if (oldPid != null) {
				if (oldPid.equals(pid))
					continue;
				CIDRSet oldCidrs = getCIDRs(pid, false);
				if (oldCidrs != null)
					oldCidrs.remove(cidr);
			}
			cidrs.add(cidr);
		}
	}
	
	/**
	 * Set or change the pid for a set of CIDRs.
	 * Note this method does not verify that the PID name is legal;
	 * the caller must do that.
	 * @param pid The new pid.
	 * @param cidrSpecs A comma or white-space separated CIDR strings.
	 * @throws IllegalStateException If this map is frozen.
	 * @throws UnknownAddressTypeException
	 *		If any of the source strings have an unknown address type prefix.
	 * @throws UnknownHostException
	 * 		If any of the source strings are not valid CIDR addresses.
	 */
	public void addCIDRs(String pid, String cidrSpecs)
			throws UnknownHostException
	{
		if (cidrSpecs != null)
			addCIDRs(pid, cidrSpecs.split("[ \t\n\r,]+"), null);
	}
	
	/**
	 * Throw an exception if "pid" is not a valid PID name.
	 * That is, if it does not conform to the rules of the ALTO RFC.
	 * This does not test if the pid has been defined in a map.
	 * @param pid A possible pid name.
	 * @throws IllegalPidNameException If "pid" isn't a legal PID name.
	 */
	public static void validatePidName(String pid) throws IllegalPidNameException
	{
		if (pid == null)
			throw new IllegalPidNameException(null, "(null)");
		int len = pid.length();
		if (len == 0)
			throw new IllegalPidNameException(null, "(blank)");
		for (int i = 0; i < len; i++) {
			char c = pid.charAt(i);
			if (c < 0x21 || c > 0x7e)
				throw new IllegalPidNameException(null, pid);
		}
		if (len > 64)
			throw new IllegalPidNameException(null, pid);
	}
	
	/**
	 * Throw an exception if "vtag" is not a valid version tag string.
	 * That is, if it does not conform to the rules of the ALTO RFC.
	 * @param vtag A possible version tag.
	 * @throws IllegalVersionTagException If "vtag" isn't a legal version tag string.
	 */
	public static void validateVersionTag(String vtag) throws IllegalVersionTagException
	{
		if (vtag == null)
			throw new IllegalVersionTagException("Null version tag");
		int len = vtag.length();
		if (len == 0)
			throw new IllegalVersionTagException("Empty version tag");
		if (len > 64)
			throw new IllegalVersionTagException("version tag \"" + vtag + " longer than 64 characters");
		for (int i = 0; i < len; i++) {
			char c = vtag.charAt(i);
			if (c < 0x21 || c > 0x7e)
				throw new IllegalVersionTagException("Invalid char 0x" + Integer.toHexString(c)
						+ " in version tag starting with \"" + vtag.substring(0,i) + "\"");
		}
		if (len > 64)
			throw new IllegalVersionTagException("Version tag \"" + vtag + "\" longer than 64 characters");
	}
	
	/**
	 * Return the set of CIDRAddresses for a pid.
	 * @param pid The PID to lookup.
	 * @param create
	 * 		If true and there are no CIDRs for this pid,
	 * 		create an empty set, bind it to the pid, and return it.
	 * 		If false, return null.
	 * 		If the map is frozen, create is always taken as false.
	 * 		We assume that if create is true,
	 *		the caller has synchronized on this object.
	 * @return
	 * 		The CIDRs for this pid. The set is live,
	 * 		so if you change it, you change the map.
	 *		However, freezing the NetworkMap freezes all CIDRSets,
	 *		so you can only change the set if the NetworkMap is still mutable.
	 */
	private CIDRSet getCIDRs(String pid, boolean create)
	{
		CIDRSet cidrs = m_pid2cidrs.get(pid);
		if (cidrs == null && create && !isFrozen())
			m_pid2cidrs.put(pid, cidrs = new CIDRSet());
		return cidrs;
	}
	
	/**
	 * Return the set of CIDRAddresses for a pid.
	 * @param pid The PID to lookup.
	 * @return
	 * 		The CIDRs for "pid". If "pid" does not exist, return null.
	 *		The NetworkMap must be frozen.
	 * @throws IllegalStateException
	 * 		If the NetworkMap is not frozen.
	 */
	public CIDRSet getCIDRs(String pid)
	{
		if (!isFrozen())
			throw new IllegalStateException("NetworkMap.getCIDRs() called before map is frozen.");
		return getCIDRs(pid, false);
	}
	
	/**
	 * Return the PID for a CIDR, or null if there is none.
	 * @param cidr The cidr to lookup.
	 * @return The PID for cidr, or null.
	 */
	public String getPID(CIDRAddress cidr)
	{
		return m_cidr2pid.get(cidr);
	}
	
	/**
	 * Return the PID for an internet address.
	 * That's the PID for the CIDR with the longest mask that covers the address.
	 * @param addr An internet address to lookup.
	 * @return The PID for that address, or null if no CIDR covers "addr".
	 */
	public String getPID(InetAddress addr)
	{
		return getPID(new EndpointAddress(addr));
	}
	
	/**
	 * Return the PID for an internet address.
	 * That's the PID for the CIDR with the longest mask that covers the address.
	 * @param addr An internet address to lookup.
	 * @return The PID for that address, or null if no CIDR covers "addr".
	 */
	public String getPID(EndpointAddress addr)
	{
		for (Map.Entry<CIDRAddress, String> ent:m_cidr2pid.entrySet()) {
			if (addr.isContainedIn(ent.getKey())) {
				return ent.getValue();
			}
		}
		return null;
	}
	
	/**
	 * Return the PIDs in this map.
	 */
	public Iterable<String> allPids()
	{
		return new IterableWrapper<String>(m_pid2cidrs.keySet());
	}
	
	/**
	 * Return a new array with all the PID names, sorted and in index order.
	 * @throws IllegalStateException If the map is not frozen.
	 */
	public String[] getPidNames()
	{
		if (m_pidNames == null)
			throw new IllegalStateException("NetworkMap.getPidNames() only works when frozen.");
		int nPids = m_pidNames.length;
		String[] ret = new String[nPids];
		for (int i = 0; i < nPids; i++) {
			ret[i] = m_pidNames[i];
		}
		return ret;
	}
	
	/**
	 * Return the PIDs and the CIDRs for each PID.
	 * In each entry, the key is the PID,
	 * and the value is the CIDRs for that PID.
	 */
	public Iterable<Map.Entry<String, CIDRSet>> allPidEntries()
	{
		return new IterableWrapper<Map.Entry<String, CIDRSet>>(m_pid2cidrs.entrySet());
	}
	
	/**
	 * Return the CIDRs and the PID for each CIDR.
	 * In each entry, the key is the CIDR,
	 * and the value is the PID for that CIDR.
	 */
	public Iterable<Map.Entry<CIDRAddress, String>> allCIDREntries()
	{
		return new IterableWrapper<Map.Entry<CIDRAddress, String>>(m_cidr2pid.entrySet());
	}
	
	/**
	 * Return the number of pids. Only works after the map has been frozen.
	 * @throws IllegalStateException If the map is not frozen.
	 */
	public int getNumPids()
	{
		if (m_pidNames == null)
			throw new IllegalStateException("NetworkMap.getNumPids() only works when frozen.");
		return m_pidNames.length;
	}
	
	/**
	 * Return the name of the n'th pid. Only works after the map has been frozen.
	 * @param index The pid index, between 0 and {@link #getNumPids()}-1.
	 * @throws IllegalStateException If the map is not frozen.
	 */
	public String indexToPid(int index)
	{
		if (m_pidNames == null)
			throw new IllegalStateException("NetworkMap.indexToPid() only works when frozen.");
		return m_pidNames[index];
	}
	
	/**
	 * Return the number of a pid. Only works after the map has been frozen.
	 * @param pid The pid name.
	 * @return The index for pid, or -1 if there is no pid with that name.
	 */
	public int pidToIndex(String pid)
	{
		if (m_pidNames == null)
			throw new IllegalStateException("NetworkMap.pidToIndex() only works when frozen.");
		if (pid == null)
			return -1;
		int v = Arrays.binarySearch(m_pidNames, pid);
		if (v >= 0)
			return v;
		else
			return -1;
	}
	
	private static final CIDRAddress m_allIPV4addrs;
	private static final CIDRAddress m_allIPV6addrs;
	static {
		CIDRAddress addr;
		try {
			addr = new CIDRAddress("0.0.0.0/0");
		} catch (UnknownHostException e) {
			addr = null;
		}
		m_allIPV4addrs = addr;
		try {
			addr = new CIDRAddress("::0/0");
		} catch (UnknownHostException e) {
			addr = null;
		}
		m_allIPV6addrs = addr;
	}
	
	/**
	 * Check whether the network map covers all addresses.
	 * Caveat: This can take a while for large maps.
	 * @return null if the map covers all addresses,
	 * 			or an error message if it does not.
	 */
	public String checkFullCoverage(boolean ipv6Required)
	{
		CIDRSet all = new CIDRSet();
		for (Map.Entry<String, CIDRSet> entry:allPidEntries()) {
			for (CIDRAddress cidr:entry.getValue()) {
				all.add(cidr);
			}
		}
		all.freeze(true);
		if (!all.contains(m_allIPV4addrs))
			return "Network map does not cover all IPV4 addresses";
		if (ipv6Required && !all.contains(m_allIPV6addrs))
			return "Network map does not cover all IPV6 addresses";
		return null;
	}
	
	/**
	 * Return a String representation of the pid=&gt;cidr mapping.
	 */
	@Override
	public String toString()
	{
		return m_mapId + ":" + m_pid2cidrs.toString();
	}
	
	/**
	 * Create a new NetworkMap from a sequence of PID names and CIDR specifications.
	 * If a string starts with "vtag=", the remainder is the map's vtag.
	 * Otherwise a CIDR has a "/", a PID name doesn't.
	 * A PID contains all the CIDRs that follow it.
	 * The CIDRs for a PID do not need to be together.
	 * If a CIDR is in more than one PID, it stays in the last PID.
	 * When done, we freeze the map.
	 * <p>
	 * In PID names, you can escape "/", "=" or "\" with "\/", "\=" or "\\".
	 * So if "127.0.0.0/24" or "vtag=x" are PID names,
	 * encode them as "127.0.0.0\/24" or "vtag\=x".
	 * 
	 * @param iter An iterator that returns PID names and CIDRs.
	 * @return A frozen NetworkMap.
	 * @throws UnknownAddressTypeException
	 * 		If a CIDR has an unknown address type prefix.
	 * @throws UnknownHostException
	 * 		If a CIDR is badly formed.
	 * @throws IllegalStateException
	 * 		If the sequence doesn't start with a PID.
	 * @throws IllegalPidNameException
	 * 		If a PID name isn't legal.
	 * @throws IllegalVersionTagException
	 * 		If the version tag isn't legal.
	 * @see FileStringIterator
	 */
	public static NetworkMap createNetworkMap(String mapId,
											  IteratorWithPosition<String> iter,
											  String makeVtagPrefix)
		throws UnknownHostException, IllegalStateException,
				IllegalPidNameException, IllegalVersionTagException
	{
		NetworkMap map = new NetworkMap(mapId);
		String pid = null;
		String vtag = null;
		for (String t:iter) {
			boolean treatAsPid = false;
			String t2;
			if ((t2 = StringUtils.removeBackslashes(t)) != t) {
				t = t2;
				treatAsPid = true;
			}
			if (!treatAsPid && t.startsWith(MAP_FILE_VTAG_PREFIX)) {
				vtag = t.substring(MAP_FILE_VTAG_PREFIX.length());
				validateVersionTag(vtag);
			} else if (!treatAsPid && t.contains("/")) {
				if (pid == null) {
					throw new IllegalStateException("Error in Network Map specification: "
													+ "Specification must start with a pid"
													+ " (" + iter.getPositionDescription() + ")");
				}
				try {
					map.addCIDRs(pid, t);
				} catch (UnknownHostException e) {
					throw new UnknownHostException("Error in Network Map specification: "
													+ e.getMessage()
													+ " (" + iter.getPositionDescription() +")");
				}
			} else {
				pid = t;
				validatePidName(pid);
			}
		}
		map.freeze(vtag, makeVtagPrefix);
		return map;
	}
	
	/**
	 * Create a set of new NetworkMaps from a sequence of PID names and CIDR specifications.
	 * If a string starts with "map-id=", the remainder is a network map resource ID.
	 * Subsequent PIDs and CIDRs define that network map.
	 * If a string starts with "vtag=", the remainder is the map's vtag.
	 * Otherwise a CIDR has a "/", a PID name doesn't.
	 * A PID contains all the CIDRs that follow it.
	 * The CIDRs for a PID do not need to be together.
	 * If a CIDR is in more than one PID, it stays in the last PID.
	 * When done, we freeze the map.
	 * <p>
	 * In PID names, you can escape "/", "=" or "\" with "\/", "\=" or "\\".
	 * So if "127.0.0.0/24" or "vtag=x" are PID names,
	 * encode them as "127.0.0.0\/24" or "vtag\=x".
	 * 
	 * @param iter An iterator that returns PID names and CIDRs.
	 * @param defaultMapId The map id to use for any entries
	 * 		before the first map-id=xxxx specification.
	 * 		If null, any such entries will be discarded.
	 * @param makeVtagPrefix Prefix for automatically created vtags (may be null).
	 * @return A list of frozen NetworkMaps. The key is the network map id.
	 * @throws UnknownAddressTypeException
	 * 		If a CIDR has an unknown address type prefix.
	 * @throws UnknownHostException
	 * 		If a CIDR is badly formed.
	 * @throws IllegalStateException
	 * 		If the sequence doesn't start with a PID.
	 * @throws IllegalPidNameException
	 * 		If a PID name isn't legal.
	 * @throws IllegalVersionTagException
	 * 		If the version tag isn't legal.
	 * @see FileStringIterator
	 */
	public static Map<String,NetworkMap> createNetworkMaps(IteratorWithPosition<String> iter,
														   String defaultMapId,
														   String makeVtagPrefix)
		throws UnknownHostException, IllegalStateException,
				IllegalPidNameException, IllegalVersionTagException
	{
		Map<String, NetworkMap> maps = new HashMap<String, NetworkMap>();
		String mapId = defaultMapId;
		NetworkMap map = new NetworkMap(mapId);
		int nCIDRs = 0;
		String pid = null;
		for (String t:iter) {
			boolean treatAsPid = false;
			String t2;
			if ((t2 = StringUtils.removeBackslashes(t)) != t) {
				t = t2;
				treatAsPid = true;
			}
			if (!treatAsPid && t.startsWith(MAP_FILE_VTAG_PREFIX)) {
				String vtag = t.substring(MAP_FILE_VTAG_PREFIX.length());
				validateVersionTag(vtag);
				map.setVtag(vtag);
			} else if (!treatAsPid && t.startsWith(MAP_FILE_MAP_ID_PREFIX)) {
				if (mapId != null && nCIDRs > 0) {
					maps.put(mapId, map);
				}
				mapId = t.substring(MAP_FILE_MAP_ID_PREFIX.length());
				if (mapId.equals(""))
					mapId = defaultMapId;
				map = new NetworkMap(mapId);
				nCIDRs = 0;
				pid = null;
			} else if (!treatAsPid && t.contains("/")) {
				if (pid == null) {
					throw new IllegalStateException("Error in Network Map specification: "
													+ "Specification must start with a pid"
													+ " (" + iter.getPositionDescription() + ")");
				}
				try {
					map.addCIDRs(pid, t);
					nCIDRs++;
				} catch (UnknownHostException e) {
					throw new UnknownHostException("Error in Network Map specification: "
													+ e + " (" + iter.getPositionDescription() +")");
				}
			} else {
				pid = t;
				validatePidName(pid);
			}
		}
		if (mapId != null && nCIDRs > 0) {
			maps.put(mapId, map);
		}
		for (NetworkMap xmap: maps.values()) {
			xmap.freeze(null, makeVtagPrefix);
		}
		return maps;
	}
	
	/**
	 *	Return an ascii string with the MD5 digest of a canonical
	 *	representation of the pids & cidrs in this map.
	 *	If we cannot do MD5 digests at all, return the current time stamp as a string.
	 *	@param prefix
	 *		If not null, prefix the returned vtag with this string.
	 *	@return
	 *		A cannonical vtag for the network map.
	 */
	private String makeVtag(String prefix)
	{
		StringBuilder hashStr = new StringBuilder();
		if (prefix != null) {
			hashStr.append(prefix);
		}
		
		MessageDigest md5Digest = null;
		try {
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (java.security.NoSuchAlgorithmException e) {
			md5Digest = null;
		}
		if (md5Digest == null) {
			hashStr.append(Long.toString(System.currentTimeMillis(), Character.MAX_RADIX));
			return hashStr.toString();
		}
		
		// NOTE: Because m_cidr2pid is a TreeMap, entrySet() returns
		// the CIDRs in predictable, repeatable order.
		for (Map.Entry<CIDRAddress, String> ent:m_cidr2pid.entrySet()) {
			ent.getKey().update(md5Digest);
			md5Digest.update((byte)' ');
			md5Digest.update(ent.getValue().getBytes());
			md5Digest.update((byte)'\n');
		}
		byte[] hash = md5Digest.digest();
		String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7",
						"8", "9", "a", "b", "c", "d", "e", "f"};
		for (byte b:hash) {
			hashStr.append(hex[(b>>4) & 0xf]);
			hashStr.append(hex[(b   ) & 0xf]);
		}
		return hashStr.toString();
	}

}
