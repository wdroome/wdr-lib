package com.wdroome.osc.qlab;

import java.util.List;
import java.io.IOException;
import java.io.PrintStream;

import com.wdroome.json.JSONValue_Object;
import com.wdroome.osc.eos.EOSUtil;
import com.wdroome.osc.eos.EOSCueNumber;

/**
 * A QLab Network Cue in QLab5 and up.
 * @author wdr
 */
public class QLabNetworkCue5 extends QLabNetworkCue
{
	public final QLabNetworkPatchInfo.PatchType m_patchType;
	public final List<String> m_parameterValues;
	
	public QLabNetworkCue5(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
			boolean isAuto, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, isAuto, queryQLab);
		QLabNetworkPatchInfo.PatchType patchType = QLabNetworkPatchInfo.PatchType.OSC_MESSAGE;
		List<String> parameterValues = null;
		if (queryQLab != null) {
			try {
				QLabNetworkPatchInfo patchInfo = queryQLab.getNetworkPatchInfo(m_patchNumber);
				patchType = patchInfo != null
								? patchInfo.m_patchType : QLabNetworkPatchInfo.PatchType.OSC_MESSAGE;
				parameterValues = queryQLab.getParameterValues(m_number);
			} catch (IOException e) {
				// ignore
			}
		}
		m_patchType = patchType;
		m_parameterValues = parameterValues != null ? parameterValues : List.of();
		setEOSCueNumber();
	}
	
	protected QLabNetworkCue5(String uniqueId, QueryQLab queryQLab)
	{
		super(uniqueId, queryQLab);
		QLabNetworkPatchInfo.PatchType patchType = QLabNetworkPatchInfo.PatchType.OSC_MESSAGE;
		List<String> parameterValues = null;
		if (queryQLab != null) {
			try {
				QLabNetworkPatchInfo patchInfo = queryQLab.getNetworkPatchInfo(m_patchNumber);
				patchType = patchInfo != null
								? patchInfo.m_patchType : QLabNetworkPatchInfo.PatchType.OSC_MESSAGE;
				parameterValues = queryQLab.getParameterValues(m_number);
			} catch (IOException e) {
				// ignore
			}
		}
		m_patchType = patchType;
		m_parameterValues = parameterValues != null ? parameterValues : List.of();
		setEOSCueNumber();
	}
	
	private void setEOSCueNumber()
	{
		EOSCueNumber eosCue = null;
		switch (m_patchType) {
		case OSC_MESSAGE:
			if (m_parameterValues.size() > QLabUtil.EOS_PARAMETER_TYPE) {
				eosCue = EOSUtil.getCueInFireRequest(m_parameterValues.get(QLabUtil.EOS_PARAMETER_TYPE));
			}
			break;
		case ETC_EOS_FAMILY:
			if (paramEquals(QLabUtil.EOS_PARAMETER_TYPE, QLabUtil.EOS_PARAMETER_TYPE_CUE)
					&& paramEquals(QLabUtil.EOS_PARAMETER_CMD, QLabUtil.EOS_PARAMETER_CMD_FIRE)
					&& m_parameterValues.size() > QLabUtil.EOS_PARAMETER_CUE_NUMBER) {
				try {
					eosCue = new EOSCueNumber(m_parameterValues.get(QLabUtil.EOS_PARAMETER_CUE_NUMBER));
				} catch (Exception e) {
					eosCue = null;
				}
			}
			break;
		default:
			// ignore.
			break;
		}
		m_eosCueNumber = eosCue;
	}
	
	private boolean paramEquals(int iParam, String value)
	{
		return iParam < m_parameterValues.size() && value.equals(m_parameterValues.get(iParam));
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " patchType=" + m_patchType + " param=" + m_parameterValues;
	}
	
	@Override
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		super.printCue(out, indent, indentIncr);
		out.println(indent + indentIncr + indentIncr + m_patchType + " param=" + m_parameterValues);
	}
}
