package org.docx4j.convert.out.common.preprocess;

import java.util.List;

import javax.xml.transform.TransformerException;

import org.docx4j.TraversalUtil;
import org.docx4j.model.fields.FldSimpleModel;
import org.docx4j.model.fields.FormattingSwitchHelper;
import org.docx4j.model.structure.HeaderFooterPolicy;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.utils.TraversalUtilVisitor;
import org.docx4j.wml.CTSimpleField;
import org.docx4j.wml.SectPr;


/** This class collects information about the use of page numbering fields,
 *  and their formatting. <br>
 *  It checks for fields of the type: PAGE, NUMPAGES, SECTIONPAGES
 */
public class PageNumberInformationCollector {
	public static PageNumberInformation process(List<Object> content, SectPr sectPr, HeaderFooterPolicy headerFooterPolicy, boolean dummyPageNumbering) {
	PageNumberInformation ret = new PageNumberInformation(sectPr);
	FieldVisitor fldVisitor = null;
		//If it is a dummyPageNumberInformation the data is read from the sectPr,
		//and it is assumed that there are no NUMPAGES and SECTIONPAGES fields
		//the result will allways be a single pass conversion.
		if (!dummyPageNumbering) {
			fldVisitor = new FieldVisitor(ret);
			TraversalUtil.visit(content, fldVisitor);
			process(headerFooterPolicy.getFirstHeader(), fldVisitor);
			process(headerFooterPolicy.getFirstFooter(), fldVisitor);
			process(headerFooterPolicy.getDefaultHeader(), fldVisitor);
			process(headerFooterPolicy.getDefaultFooter(), fldVisitor);
			process(headerFooterPolicy.getEvenHeader(), fldVisitor);
			process(headerFooterPolicy.getEvenFooter(), fldVisitor);
		}
		return ret;
	}
	
	protected static void process(FooterPart footer, FieldVisitor fldVisitor) {
		if ((footer != null) && (!footer.getContent().isEmpty())) {
			TraversalUtil.visit(footer.getContent(), fldVisitor);
		}
	}
	
	protected static void process(HeaderPart header, FieldVisitor fldVisitor) {
		if ((header != null) && (!header.getContent().isEmpty())) {
			TraversalUtil.visit(header.getContent(), fldVisitor);
		}
	}

	protected static class FieldVisitor extends TraversalUtilVisitor<CTSimpleField> {
		
		private static final Object PAGE_FIELD_TYPE = "PAGE";
		private static final Object NUMPAGES_FIELD_TYPE = "NUMPAGES";
		private static final Object SECTIONPAGES_FIELD_TYPE = "SECTIONPAGES";
		
		protected PageNumberInformation results = null;
		protected FldSimpleModel fldSimpleModel = new FldSimpleModel();
		
		public FieldVisitor(PageNumberInformation results) {
			this.results = results;
		}

		@Override
		public void apply(CTSimpleField element) {
			
			String instr = element.getInstr();
			
			//String fieldType = FormattingSwitchHelper.getFldSimpleName(instr);
			try {
				fldSimpleModel.build(instr); // TODO: do this once only
			} catch (TransformerException e) {
				e.printStackTrace();
			} 
			String fieldType = fldSimpleModel.getFldType();
			
			if (PAGE_FIELD_TYPE.equals(fieldType)) {
				results.setPagePresent(true);
				results.setPageFormat(extractFormat(instr));
			}
			else if (NUMPAGES_FIELD_TYPE.equals(fieldType)) {
				results.setNumpagesPresent(true);
				results.setNumpagesFormat(extractFormat(instr));
			}
			else if (SECTIONPAGES_FIELD_TYPE.equals(fieldType)) {
				results.setSectionpagesPresent(true);
				results.setSectionpagesFormat(extractFormat(instr));
			}
		}

		protected String extractFormat(String instr) {
		String ret = null;
			try {
				fldSimpleModel.build(instr);
				return FormattingSwitchHelper.findFormat("\\*", fldSimpleModel.getFldParameters());
			} catch (TransformerException e) {
				ret = null;
			}
			return ret;
		}
		
	}
}
