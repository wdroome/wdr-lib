package com.wdroome.apps.midi2osc;


/**
 * An instantiated action to be sent to the OSC server.
 * There is a many-to-one relationship between Action and {@link ActionSpec}.
 * @author wdr
 */
public class Action
{
	public final ActionSpec m_actionSpec;
	public final int m_data2;
	public final long m_arriveTS;
	
	private long m_holdTS = 0;
	private long m_sentTS = 0;
	
	public Action(ActionSpec actionSpec, int data2, long arriveTS)
	{
		m_actionSpec = actionSpec;
		m_data2 = data2;
		m_arriveTS = arriveTS;
	}

	public long getHoldTS() {
		return m_holdTS;
	}

	public void setHoldTS(long holdTS) {
		this.m_holdTS = holdTS;
	}

	public long getSentTS() {
		return m_sentTS;
	}

	public void setSentTS(long sentTS) {
		this.m_sentTS = sentTS;
	}
	
	public int getSameValueSkipMS() {
		return m_actionSpec.m_sameValueSkipMS;
	}
	
	public int getNewValueWaitMS() {
		return m_actionSpec.m_newValueWaitMS;
	}

	@Override
	public String toString() {
		return "Action["
				+ "server=\"" + m_actionSpec.m_oscServer.m_name + "\""
				+ "tck=" + m_actionSpec.m_encodedTCK
				+ ", data2=" + m_data2
				+ ", arriveTS=" + m_arriveTS
				+ ", cmds=" + m_actionSpec.m_cmdTemplate
				+ ", new/sameMS=" + m_actionSpec.m_newValueWaitMS + "/" + m_actionSpec.m_sameValueSkipMS
				+ "]";
	}
}
