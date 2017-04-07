package org.gjt.jclasslib.tools;

import org.gjt.jclasslib.bytecode.AbstractBranchInstruction;
import org.gjt.jclasslib.bytecode.AbstractInstruction;
import org.gjt.jclasslib.bytecode.ImmediateByteInstruction;
import org.gjt.jclasslib.bytecode.ImmediateShortInstruction;
import org.gjt.jclasslib.bytecode.LookupSwitchInstruction;
import org.gjt.jclasslib.bytecode.MatchOffsetPair;
import org.gjt.jclasslib.bytecode.Opcodes;
import org.gjt.jclasslib.bytecode.OpcodesUtil;
import org.gjt.jclasslib.bytecode.TableSwitchInstruction;
import org.gjt.jclasslib.io.ByteCodeReader;
import org.gjt.jclasslib.io.ClassFileReader;
import org.gjt.jclasslib.structures.AttributeInfo;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.FieldInfo;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.MethodInfo;
import org.gjt.jclasslib.structures.attributes.CodeAttribute;
import org.gjt.jclasslib.structures.attributes.ConstantValueAttribute;
import org.gjt.jclasslib.structures.attributes.ExceptionTableEntry;
import org.gjt.jclasslib.structures.attributes.SignatureAttribute;
import org.gjt.jclasslib.structures.constants.ConstantClassInfo;
import org.gjt.jclasslib.structures.constants.ConstantDoubleInfo;
import org.gjt.jclasslib.structures.constants.ConstantFieldrefInfo;
import org.gjt.jclasslib.structures.constants.ConstantFloatInfo;
import org.gjt.jclasslib.structures.constants.ConstantIntegerInfo;
import org.gjt.jclasslib.structures.constants.ConstantInterfaceMethodrefInfo;
import org.gjt.jclasslib.structures.constants.ConstantLongInfo;
import org.gjt.jclasslib.structures.constants.ConstantMethodHandleInfo;
import org.gjt.jclasslib.structures.constants.ConstantMethodTypeInfo;
import org.gjt.jclasslib.structures.constants.ConstantMethodrefInfo;
import org.gjt.jclasslib.structures.constants.ConstantNameAndTypeInfo;
import org.gjt.jclasslib.structures.constants.ConstantStringInfo;
import org.gjt.jclasslib.structures.constants.ConstantUtf8Info;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by chenjinyi on 16-5-27.
 */
public class DecodeClass {
    private static ClassFile classFile;
    private static StringBuffer classContent;
    private static CPInfo[] cpInfoPool;

    public static String decodeclass(String classPath) {
        classContent = new StringBuffer();
        try {
            classFile = ClassFileReader.readFromFile(new File(classPath));
            cpInfoPool = classFile.getConstantPool();
            /* class is public or private */
            classContent.append(classFile.getAccessFlags());
            classContent.append(" ");
            /* class name */
            classContent.append(classFile.getThisClassName());
            classContent.append(" ");
            /* super class */
            classContent.append("extends ");
            classContent.append(classFile.getSuperClassName());
            classContent.append(" ");
            /* interface */
            int[] interfaceindex = classFile.getInterfaces();
            classContent.append("implements ");
            for(int index : interfaceindex) {
                classContent.append(((ConstantClassInfo) cpInfoPool[index]).getName());
                classContent.append(",");
            }
            classContent.append("\n");
            /* fileds */
            classContent.append("fileds ");
            FieldInfo[] fieldInfos = classFile.getFields();
            classContent.append(fieldInfos.length);
            classContent.append("\n");
            for(FieldInfo field : fieldInfos) {
                classContent.append(field.getAccessFlagsVerbose());
                classContent.append(" ");
                classContent.append(field.getDescriptor());
                classContent.append(" ");
                classContent.append(field.getName());
                classContent.append("=");
                for(AttributeInfo attr : field.getAttributes()) {
                    int index = getFieldAttributeIndex(attr);
                    if(index == -1) continue;
                    CPInfo cp = cpInfoPool[index];
                    classContent.append(getFieldValue(cp));
                }
                classContent.append("\n");
            }
            /* methods */
            classContent.append("methods ");
            MethodInfo[] methodInfos = classFile.getMethods();
            classContent.append(methodInfos.length);
            classContent.append("\n");
            for(MethodInfo method : methodInfos) {

                classContent.append(method.getAccessFlagsVerbose());
                classContent.append(" ");
                classContent.append(method.getDescriptor());
                classContent.append(" ");
                classContent.append(method.getName());
                classContent.append(" {\n");
                for(AttributeInfo attr : method.getAttributes()) {
                    ArrayList instructions = getMethodCode(attr);
                    if(instructions == null) continue;
                    Iterator it = instructions.iterator();
                    AbstractInstruction currentInstruction;
                    while (it.hasNext()) {
                        classContent.append("  ");
                        currentInstruction = (AbstractInstruction) it.next();
                        classContent.append(decodeInstruction(currentInstruction, instructions));
                        classContent.append("\n");
                    }
                }
                classContent.append("}\n");
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
        return classContent.toString();
    }

    private static String getCpValueByIndex(int index) {
        String value = "_";
        if(index >= cpInfoPool.length) return value;
        try {
            value = getCpValue(cpInfoPool[index]);
        }catch (InvalidByteCodeException e) {
            e.printStackTrace();
        }
        return value;
    }

    private static String getCpValue(CPInfo cp) throws InvalidByteCodeException {
        String value = "";
        switch (cp.getTag()) {
            case CPInfo.CONSTANT_CLASS:
                value = ((ConstantClassInfo) cp).getName();
                break;
            case CPInfo.CONSTANT_FIELDREF:
                value = getCpValueByIndex(((ConstantFieldrefInfo) cp).getClassIndex()) +
                        getCpValueByIndex(((ConstantFieldrefInfo) cp).getNameAndTypeIndex());
                break;
            case CPInfo.CONSTANT_METHODREF:
                value = getCpValueByIndex(((ConstantMethodrefInfo) cp).getClassIndex()) +
                        getCpValueByIndex(((ConstantMethodrefInfo) cp).getNameAndTypeIndex());
                break;
            case CPInfo.CONSTANT_INTERFACE_METHODREF:
                value = getCpValueByIndex(((ConstantInterfaceMethodrefInfo) cp).getNameAndTypeIndex());
                break;
            case CPInfo.CONSTANT_NAME_AND_TYPE:
                value = ((ConstantNameAndTypeInfo) cp).getDescriptor() + " " + ((ConstantNameAndTypeInfo) cp).getName();
                break;
            case CPInfo.CONSTANT_METHOD_HANDLE:
                value = ((ConstantMethodHandleInfo) cp).getName();
                break;
            case CPInfo.CONSTANT_METHOD_TYPE:
                value = ((ConstantMethodTypeInfo) cp).getName();
                break;
            case CPInfo.CONSTANT_UTF8:
                value = ((ConstantUtf8Info) cp).getString();
                break;
            default:
                value = getFieldValue(cp);
        }
        return value;
    }

    private static String getFieldValue(CPInfo cp) throws InvalidByteCodeException {
        String value = "";
        switch (cp.getTag()) {
            case CPInfo.CONSTANT_STRING:
                value = classFile.getConstantPoolUtf8Entry(((ConstantStringInfo) cp).getStringIndex()).getString();
                break;
            case CPInfo.CONSTANT_INTEGER:
                value = Integer.toString(((ConstantIntegerInfo) cp).getInt());
                break;
            case CPInfo.CONSTANT_FLOAT:
                value = Float.toString(((ConstantFloatInfo) cp).getFloat());
                break;
            case CPInfo.CONSTANT_LONG:
                value = Long.toString(((ConstantLongInfo) cp).getLong());
                break;
            case CPInfo.CONSTANT_DOUBLE:
                value = Double.toString(((ConstantDoubleInfo) cp).getDouble());
                break;
        }
        return value;
    }

    private static int getFieldAttributeIndex(AttributeInfo attr) throws InvalidByteCodeException {
        int value = -1;
        String attributeName = attr.getName();
        if (ConstantValueAttribute.ATTRIBUTE_NAME.equals(attributeName)) {
            value = ((ConstantValueAttribute) attr).getConstantvalueIndex();
        } else if (SignatureAttribute.ATTRIBUTE_NAME.equals(attributeName)) {
            value = ((SignatureAttribute) attr).getSignatureIndex();
        }
        return value;
    }

    private static ArrayList getMethodCode(AttributeInfo attr) throws InvalidByteCodeException {
        ArrayList instructions = null;
        String attributeName = attr.getName();
        if (CodeAttribute.ATTRIBUTE_NAME.equals(attributeName)) {
            try {
                byte[] code = ((CodeAttribute) attr).getCode();
                instructions = ByteCodeReader.readByteCode(code);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instructions;
    }

    private static String getMethodException(AttributeInfo attr) throws InvalidByteCodeException {
        String exceptions = "";
        String attributeName = attr.getName();
        if (CodeAttribute.ATTRIBUTE_NAME.equals(attributeName)) {
            try {
                ExceptionTableEntry[] exceptionTable = ((CodeAttribute) attr).getExceptionTable();
                for(ExceptionTableEntry ex : exceptionTable) {
                    exceptions = exceptions+String.valueOf(ex.getCatchType());
                    exceptions = exceptions + "_";
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return exceptions;
    }

    private static String decodeInstruction(AbstractInstruction instruction, ArrayList instructions) {

        String value = "_";
        if (instruction instanceof ImmediateByteInstruction) {
            value = getImmediateByteSpecificInfo((ImmediateByteInstruction)instruction);
        } else if (instruction instanceof ImmediateShortInstruction) {
            value = getImmediateShortSpecificInfo((ImmediateShortInstruction)instruction);
        } else if (instruction instanceof AbstractBranchInstruction) {
            value = getBranchSpecificInfo((AbstractBranchInstruction)instruction, instructions);
        } else if (instruction instanceof TableSwitchInstruction) {
            value = getTableSwitchSpecificInfo((TableSwitchInstruction)instruction);
        } else if (instruction instanceof LookupSwitchInstruction) {
            value = getLookupSwitchSpecificInfo((LookupSwitchInstruction)instruction, instructions);
        }else {
            value = instruction.getOpcodeVerbose();
        }
        return value;
    }

    private static String getImmediateByteSpecificInfo(ImmediateByteInstruction instruction) {

        String opstr = "ImmediateByte:"+instruction.getOpcodeVerbose();
        opstr = opstr + " ";
        int opcode = instruction.getOpcode();
        int immediateByte = instruction.getImmediateByte();

        if (opcode == Opcodes.OPCODE_LDC) {
            opstr = opstr + getCpValueByIndex(immediateByte);
        } else if (opcode == Opcodes.OPCODE_NEWARRAY) {
            String verbose = OpcodesUtil.getArrayTypeVerbose(immediateByte);
            opstr = opstr + verbose;
        } else if (opcode == Opcodes.OPCODE_BIPUSH) {
            opstr = opstr + String.valueOf(immediateByte);
        } else {
            opstr = opstr + String.valueOf(immediateByte);
        }
        return opstr;
    }

    private static String getImmediateShortSpecificInfo(ImmediateShortInstruction instruction) {

        int opcode = instruction.getOpcode();
        int immediateShort = instruction.getImmediateShort();
        String opstr = "ImmediateShort:"+instruction.getOpcodeVerbose();
        opstr = opstr + " ";
        if (opcode == Opcodes.OPCODE_SIPUSH) {
            opstr = opstr + String.valueOf(immediateShort);
        } else {
            opstr = opstr + getCpValueByIndex(immediateShort);
        }

        return opstr;
    }

    /**
     * 跳转语句
     * 为避免跳转路径不同，直接取目标语句
     * */
    private static String getBranchSpecificInfo(AbstractBranchInstruction instruction, ArrayList instructions) {

        int branchOffset = instruction.getBranchOffset();
        int offest = instruction.getOffset();
        //String opstr = "branchSpecific:"+instruction.getOpcodeVerbose();
        String opstr = "";
        Iterator it = instructions.iterator();
        AbstractInstruction currentInstruction;
        while (it.hasNext()) {
            currentInstruction = (AbstractInstruction) it.next();
            if(currentInstruction.getOffset() == branchOffset + offest) {
                opstr = opstr + decodeInstruction(currentInstruction, instructions);
            }
        }
        return opstr;
    }

    private static String getTableSwitchSpecificInfo(TableSwitchInstruction instruction) {

        int lowByte = instruction.getLowByte();
        int highByte = instruction.getHighByte();
        int[] jumpOffsets = instruction.getJumpOffsets();

        String opstr = "TableSwitch:"+instruction.getOpcodeVerbose();;
        opstr = opstr + String.valueOf(lowByte);
        opstr = opstr + " ";
        opstr = opstr + String.valueOf(highByte);
        for(int data : jumpOffsets) {
            opstr = opstr + " ";
            opstr = opstr + String.valueOf(data);
            opstr = opstr + " ";
        }
        return opstr;
    }

    private static String getLookupSwitchSpecificInfo(LookupSwitchInstruction instruction, ArrayList instructions) {

        java.util.List matchOffsetPairs = instruction.getMatchOffsetPairs();
        int matchOffsetPairsCount = matchOffsetPairs.size();
        int branchOffset = instruction.getOffset();

        String opstr = "LookupSwitch:"+instruction.getOpcodeVerbose();
        opstr = opstr + " ";
        opstr = opstr + String.valueOf(matchOffsetPairsCount);
        opstr = opstr + " ";
        for (int i = 0; i < matchOffsetPairs.size(); i++) {
            MatchOffsetPair matchOffsetPair = (MatchOffsetPair)matchOffsetPairs.get(i);
            opstr = opstr + String.valueOf(matchOffsetPair.getMatch());
            opstr = opstr + " ";

            Iterator it = instructions.iterator();
            AbstractInstruction currentInstruction;
            while (it.hasNext()) {
                currentInstruction = (AbstractInstruction) it.next();
                if(currentInstruction.getOffset() == branchOffset + matchOffsetPair.getOffset()) {
                    opstr = opstr + decodeInstruction(currentInstruction, instructions);
                }
            }

            opstr = opstr + " ";
        }
        return opstr;
    }
}
