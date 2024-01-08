/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.intergral.deep.agent.tracepoint.inst.asm;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.ASM8;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

import com.google.common.collect.Sets;
import com.intergral.deep.agent.tracepoint.inst.asm.Visitor.MappedMethod.MappedVar;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.TracePointConfig.EStage;
import java.com.intergral.deep.ProxyCallback;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This visitor is the main magic of deep. It deals with install the callbacks into the user code, based on the tracepoints.
 */
public class Visitor extends ClassVisitor {

  // these are the local variables that we want to capture when using CF, if we try to get all locals we get verify errors.
  // and we do not care about all the locals for CF.
  private static final List<String> CF_VARS = Arrays.asList("__localScope",
      "instance",
      "__arguments",
      "this",
      "parentPage");
  private static final Logger LOGGER = LoggerFactory.getLogger(Visitor.class);
  private static final SkipException EXCEPTION = new SkipException();
  private static final boolean DEBUG = Boolean.getBoolean("DEEP_VISITOR_DEBUG");
  private static final boolean INST_PRINTER_ENABLED = Boolean.getBoolean("DEEP_INTS_PRINTER_ENABLED");

  private final Collection<TracePointConfig> bps;
  private final boolean isCf;
  private final Map<Long, List<TracePointConfig>> lineNos = new HashMap<>();
  private final Map<String, List<TracePointConfig>> methodBPs = new HashMap<>();

  private final List<MappedMethod> mappedMethods = new ArrayList<>();

  private String classname;
  private String superName;
  private String filename;

  private boolean changed = false;

  public static final Class<?> CALLBACK_CLASS;

  static {
    // this is here to make the tests easier.
    // we cannot use java. classes in the tests without screwing with the class loaders
    // so in the tests we use the 'deep.callback.class' which is the CallBack.class
    // at runtime we use the ProxyCallback.class, so we can bypass the osgi classloading restrictions
    final String property = System.getProperty("deep.callback.class");
    if (property == null) {
      CALLBACK_CLASS = ProxyCallback.class;
    } else {
      Class<?> callbackClass;
      try {
        callbackClass = Class.forName(property);
      } catch (ClassNotFoundException e) {
        callbackClass = ProxyCallback.class;
      }
      CALLBACK_CLASS = callbackClass;
    }
  }

  /**
   * Create a new visitor.
   *
   * @param v    the asm visitor that calls this
   * @param bps  the tracepoints we want to install
   * @param isCf is this a cf class
   */
  public Visitor(final ClassVisitor v, final Collection<TracePointConfig> bps, final boolean isCf) {
    super(ASM8, v);
    this.bps = bps;
    this.isCf = isCf;
    for (final TracePointConfig bp : bps) {
      final long lineNo = bp.getLineNo();
      // method tracepoints do not have to define line numbers
      if (lineNo != -1) {
        final List<TracePointConfig> list = lineNos.computeIfAbsent(lineNo, k -> new ArrayList<>());

        list.add(bp);
      }
      // if we have a method name then track that
      final String methodName = bp.getArg(TracePointConfig.METHOD_NAME, String.class, null);
      if (methodName != null) {
        final List<TracePointConfig> list = this.methodBPs.computeIfAbsent(methodName, k -> new ArrayList<>());
        list.add(bp);
      }
    }
  }


  public boolean wasChanged() {
    return changed;
  }


  @Override
  public void visit(int version, int access, String name, String signature, String superName,
      String[] interfaces) {
    LOGGER.debug("visit {}", name);
    this.classname = name;
    this.superName = superName;
    super.visit(version, access, name, signature, superName, interfaces);
  }


  @Override
  public void visitSource(String source, String debug) {
    LOGGER.debug("visitSource {} {}", classname, source);
    super.visitSource(source, debug);

    // No source filename
    if (source == null) {
      throw EXCEPTION;
    }

    filename = source;
  }

  /**
   * Converts primatives to objects so we can put them in the map.
   *
   * @param index      variable index
   * @param clazz      variable class
   * @param loadOpcode opcode
   * @param primitive  is primitive variable
   * @return the instruction list
   */
  private static InsnList box(final int index, final Type primitive, final Class<?> clazz,
      final int loadOpcode) {
    final InsnList boxOperations = new InsnList();
    boxOperations.add(new VarInsnNode(loadOpcode, index));
    // Call Double.valueOf or Long.valueOf etc
    boxOperations.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(clazz), "valueOf",
        Type.getMethodDescriptor(Type.getType(clazz), primitive), false));
    return boxOperations;
  }

  /**
   * Generates a XXLOAD operation for a specific variable slot type.
   *
   * @param t     variable type
   * @param index variable index
   * @return the instruction list
   */
  static InsnList loadVariable(final Type t, final int index) {
    final int sort = t.getSort();
    switch (sort) {
      case Type.BYTE:
        return box(index, Type.BYTE_TYPE, Byte.class, ILOAD);
      case Type.CHAR:
        return box(index, Type.CHAR_TYPE, Character.class, ILOAD);
      case Type.DOUBLE:
        return box(index, Type.DOUBLE_TYPE, Double.class, DLOAD);
      case Type.FLOAT:
        return box(index, Type.FLOAT_TYPE, Float.class, FLOAD);
      case Type.INT:
        return box(index, Type.INT_TYPE, Integer.class, ILOAD);
      case Type.LONG:
        return box(index, Type.LONG_TYPE, Long.class, LLOAD);
      case Type.SHORT:
        return box(index, Type.SHORT_TYPE, Short.class, ILOAD);
      case Type.BOOLEAN:
        return box(index, Type.BOOLEAN_TYPE, Boolean.class, ILOAD);
      case Type.ARRAY:
      case Type.OBJECT:
        final InsnList ops = new InsnList();
        ops.add(new VarInsnNode(ALOAD, index));
        return ops;
      default:
        // Something is very strange
        LOGGER.error("loadVariable - Unknown type : {}", t);
        final InsnList valueGetter = new InsnList();
        valueGetter.add(new LdcInsnNode("Unknown type :" + t));
        return valueGetter;
    }
  }

  private boolean isSuperCall(final AbstractInsnNode node) {
    // need a method node
    if (!(node instanceof MethodInsnNode)) {
      return false;
    }

    // we need to use INVOKESPECIAL on constructors
    if (node.getOpcode() != INVOKESPECIAL) {
      return false;
    }

    // all constructors have the same name
    if (!((MethodInsnNode) node).name.equals("<init>")) {
      return false;
    }

    // check the super name against this call
    return ((MethodInsnNode) node).owner.equals(this.superName);
  }

  private boolean isThrow(final AbstractInsnNode node, final LabelNode start) {
    if (start == null) {
      return false;
    }

    if (!(node instanceof InsnNode)) {
      return false;
    }

    final int opcode = node.getOpcode();
    return opcode == ATHROW;
  }

  private boolean isNextLine(final AbstractInsnNode node, final LabelNode start) {
    if (start == null) {
      return false;
    }

    return node.getType() == AbstractInsnNode.LINE;
  }

  private boolean isReturn(final AbstractInsnNode node, final LabelNode start) {
    if (start == null) {
      return false;
    }

    if (!(node instanceof InsnNode)) {
      return false;
    }

    final int opcode = node.getOpcode();
    switch (opcode) {
      case Opcodes.RETURN:
      case IRETURN:
      case Opcodes.LRETURN:
      case Opcodes.FRETURN:
      case Opcodes.ARETURN:
      case Opcodes.DRETURN:
        return true;
      default:
        return false;
    }
  }

  private boolean isStatic(final int access) {
    return (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
  }

  private boolean noneOrJustThis(final List<LocalVariableNode> localVariables) {
    if (localVariables.isEmpty()) {
      return true;
    }
    if (localVariables.size() == 1) {
      return localVariables.get(0).name.equals("this");
    }
    return false;
  }

  static String determineNewMethodName(final String name) {
    return "$deep$" + name;
  }

  /**
   * We make the replaced methods private, so we can correct overridden methods.
   *
   * @return the access flags for the method
   * @see Opcodes
   */
  static int replacedMethodAcc(final boolean isStatic) {
    if (TransformerUtils.USE_SYNTHETIC) {
      return ACC_PRIVATE + ACC_SYNTHETIC + (isStatic ? ACC_STATIC : 0);
    }
    return ACC_PRIVATE + (isStatic ? ACC_STATIC : 0);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exps) {

    // if the method is abstract then we cannot inject tracepoints - so skip it
    if (TransformerUtils.isAbstract(access)) {
      return super.visitMethod(access, name, desc, signature, exps);
    }

    LOGGER.debug("visitMethod {} {}", classname, name);

    // if we have a method tracepoint for this method name then rename the method
    final String methodName;
    final int methodAccess;
    // we use atomic as we have to update this later
    final AtomicBoolean isMapped = new AtomicBoolean(false);
    final List<TracePointConfig> tracePointConfigs = this.methodBPs.get(name);
    final MappedMethod mappedMethod = new MappedMethod(access, name, desc, signature, exps, -1);
    // if we have a method tracepoint for this method name then rename the method.
    if (tracePointConfigs != null) {
      // rename the method and make it private synthetic $deep$..
      methodName = determineNewMethodName(name);
      methodAccess = replacedMethodAcc(TransformerUtils.isStatic(access));
      // record the change so we can fix them up later
      this.mappedMethods.add(mappedMethod);
      isMapped.set(true);
    } else {
      methodName = name;
      methodAccess = access;
    }

    // MethodNode used to handle the maxes for us to make it simpler
    return new MethodNode(ASM7, methodAccess, methodName, desc, signature, exps) {

      Label startLabel;

      @Override
      public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end,
          final int index) {
        if (startLabel != null && startLabel.equals(start)) {
          mappedMethod.acceptVariable(name, descriptor, index);
        }
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
      }

      @Override
      public void visitLineNumber(final int line, final Label start) {
        mappedMethod.acceptLine(line);
        if (startLabel == null) {
          startLabel = start;
        }
        super.visitLineNumber(line, start);
      }

      @Override
      public void visitEnd() {
        // we need the var offset so we can ensure we do not reuse var slots while handling catch blocks
        // we start as the max Locals (the max vars within any frame).
        int varOffset = this.maxLocals;

        // Cannot go from line number label offset to label offset as getOffset throws.
        // java.lang.IllegalStateException: Label offset position has not been resolved yet
        // no idea why as the java local var table knows exactly at what byte code op vars are valid
        final Set<Label> seenLabels = Sets.newIdentityHashSet();

        // the start label for the line to wrap
        LabelNode start = null;
        LineNumberNode ln = null;

        // we need to handle constructors a little differently
        final boolean isConstructor = name.equals("<init>");
        boolean hasCalledSuper = false;
        LineNumberNode constructorLine = null;
        final List<TracePointConfig> iBreakpoints = new ArrayList<>();

        final ListIterator<AbstractInsnNode> it = instructions.iterator();
        while (it.hasNext()) {
          AbstractInsnNode node = it.next();

          if (isConstructor && !hasCalledSuper && isSuperCall(node)) {
            hasCalledSuper = true;
          }

          final boolean isReturnNode = isReturn(node, start);
          final boolean isThrowNode = isThrow(node, start);
          final boolean isNextLine = isNextLine(node, start);

          // we do not support line end in cf atm
          if (!isCf && (isNextLine || isReturnNode || isThrowNode)) {
            // we get the previous node here as the possible remove can mess it up later
            final AbstractInsnNode previous = node.getPrevious();

            if (isReturnNode) {
              // remove the current node as we need to change it to return in our finally block
              it.remove();
            }

            final InsnList hook = new InsnList();

            Label endOfTry = new Label();
            Label catchStart = new Label();
            Label startFinally = new Label();
            Label endCatch = new Label();
            // add the try/catch around the original code
            tryCatchBlocks.add(new TryCatchBlockNode(start,
                new LabelNode(endOfTry),
                new LabelNode(catchStart),
                "java/lang/Throwable"));
            // add try/finally for the original code
            tryCatchBlocks.add(
                new TryCatchBlockNode(start,
                    new LabelNode(endOfTry),
                    new LabelNode(startFinally),
                    null));
            // add try/finally for the catch code
            tryCatchBlocks.add(
                new TryCatchBlockNode(new LabelNode(catchStart), new LabelNode(endCatch),
                    new LabelNode(startFinally), null));
            // 'good' finally {
            // store the return value if we have one
            if (isReturnNode && node.getOpcode() != RETURN) {
              // we have to store the return but we need to use the correct store code
              // we use the return code to find the correct store code
              final int opStore;
              switch (node.getOpcode()) {
                case IRETURN:
                  opStore = ISTORE;
                  break;
                case Opcodes.LRETURN:
                  opStore = LSTORE;
                  break;
                case Opcodes.FRETURN:
                  opStore = FSTORE;
                  break;
                case Opcodes.ARETURN:
                  opStore = ASTORE;
                  break;
                case Opcodes.DRETURN:
                  opStore = DSTORE;
                  break;
                default:
                  // not really possible
                  LOGGER.debug("Unknown op code {}", node.getOpcode());
                  return;
              }
              hook.add(new VarInsnNode(opStore, varOffset));
            }
            hook.add(new LabelNode(endOfTry));

            // list = new ArrayList()
            hook.add(new TypeInsnNode(NEW, Type.getInternalName(ArrayList.class)));
            hook.add(new InsnNode(DUP));
            hook.add(
                new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>",
                    "()V", false));

            for (final TracePointConfig bp : iBreakpoints) {
              if (!bp.acceptStage(EStage.LINE_END)) {
                continue;
              }
              // list.add(bp.getId());
              hook.add(new InsnNode(DUP)); // we need a ptr to our map
              hook.add(new LdcInsnNode(bp.getId())); // bp id
              hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
                  "(Ljava/lang/Object;)Z"));
              hook.add(new InsnNode(POP)); // dont care about return
            }

            hook.add(new LdcInsnNode(filename));
            if (ln != null) {
              hook.add(new LdcInsnNode(ln.line));
            } else {
              hook.add(new LdcInsnNode(-1));
            }
            // if we are a next line then we need to remove the start line label from the seen labels so
            // the variable capture in the finally does not capture variables defined on the line we are wrapping
            if (isNextLine) {
              seenLabels.remove(((LineNumberNode) node).start.getLabel());
            }
            processLocalVariables(seenLabels, hook);
            // add the start label again so we dont break subsequent tracepoints
            if (isNextLine) {
              seenLabels.add(((LineNumberNode) node).start.getLabel());
            }

            // Callback.callBackFinally(set)
            hook.add(new MethodInsnNode(INVOKESTATIC,
                Type.getInternalName(CALLBACK_CLASS),
                "callBackFinally",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                    Type.getType(List.class),
                    Type.getType(String.class),
                    Type.getType(int.class),
                    Type.getType(Map.class)),
                false));
            if (isReturnNode) {
              // if we are not a void return the reload the var
              if (node.getOpcode() != RETURN) {
                // we have to load the return but we need to use the correct load code
                // we use the return code to find the correct load code
                final int opLoad;
                switch (node.getOpcode()) {
                  case IRETURN:
                    opLoad = ILOAD;
                    break;
                  case Opcodes.LRETURN:
                    opLoad = LLOAD;
                    break;
                  case Opcodes.FRETURN:
                    opLoad = FLOAD;
                    break;
                  case Opcodes.ARETURN:
                    opLoad = ALOAD;
                    break;
                  case Opcodes.DRETURN:
                    opLoad = DLOAD;
                    break;
                  default:
                    // not really possible
                    LOGGER.debug("Unknown op code {}", node.getOpcode());
                    return;
                }
                // load and return
                hook.add(new VarInsnNode(opLoad, varOffset));
                // increment varOffset to account for return value
                final Type returnType = Type.getReturnType(this.desc);
                char charType = returnType.getDescriptor().charAt(0);
                // J (long) and D (double) take 2 slots
                if (charType == 'J' || charType == 'D') {
                  varOffset += 2;
                } else {
                  varOffset += 1;
                }
              }
              // insert the current return node
              hook.add(node);
            } else if (isNextLine) {
              final LineNumberNode lineNumberNode = (LineNumberNode) node;
              final LabelNode nextLineStart = lineNumberNode.start;
              // goto next line
              hook.add(new JumpInsnNode(GOTO, nextLineStart));
            }
            // } (end finally)
            hook.add(new LabelNode(catchStart));
            // store 'current' (will be exception) var in next slot
            hook.add(new VarInsnNode(ASTORE, varOffset));
            // set label for start catch block
            Label startCatchLabel = new Label();
            // catch (Throwable t) {
            hook.add(new LabelNode(startCatchLabel));
            // load exception
            hook.add(new VarInsnNode(ALOAD, varOffset));
            // Callback.callBackException(t);
            hook.add(new MethodInsnNode(INVOKESTATIC,
                Type.getInternalName(CALLBACK_CLASS),
                "callBackException",
                "(Ljava/lang/Throwable;)V",
                false));
            // load exception again (callback consumes it)
            hook.add(new VarInsnNode(ALOAD, varOffset));
            // re throw the exception
            hook.add(new InsnNode(ATHROW));
            // } (end catch)
            hook.add(new LabelNode(
                startFinally)); // we start the 'finally' before we end the catch for some reason
            // 'bad' finally {
            // store exception in next slot
            hook.add(new VarInsnNode(ASTORE, varOffset + 1));
            // finally {
            hook.add(new LabelNode(endCatch));

            // set = new HashSet()
            hook.add(new TypeInsnNode(NEW, Type.getInternalName(ArrayList.class)));
            hook.add(new InsnNode(DUP));
            hook.add(
                new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>",
                    "()V", false));

            for (final TracePointConfig bp : iBreakpoints) {
              if (!bp.acceptStage(EStage.LINE_END)) {
                continue;
              }
              // set.add(bp.getId());
              hook.add(new InsnNode(DUP)); // we need a ptr to our map
              hook.add(new LdcInsnNode(bp.getId())); // bp id
              hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
                  "(Ljava/lang/Object;)Z"));
              hook.add(new InsnNode(POP)); // don't care about return
            }

            hook.add(new LdcInsnNode(filename));
            if (ln != null) {
              hook.add(new LdcInsnNode(ln.line));
            } else {
              hook.add(new LdcInsnNode(-1));
            }
            // if we are a next line then we need to remove the start line label from the seen labels so
            // the variable capture in the finally does not capture variables defined on the line we are wrapping
            if (isNextLine) {
              seenLabels.remove(((LineNumberNode) node).start.getLabel());
            }
            processLocalVariables(seenLabels, hook);
            // add the start label again so we dont break subsequent tracepoints
            if (isNextLine) {
              seenLabels.add(((LineNumberNode) node).start.getLabel());
            }
            // Callback.callBackFinally(set)
            hook.add(new MethodInsnNode(INVOKESTATIC,
                Type.getInternalName(CALLBACK_CLASS),
                "callBackFinally",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                    Type.getType(List.class),
                    Type.getType(String.class),
                    Type.getType(int.class),
                    Type.getType(Map.class)),
                false));
            // load exception back
            hook.add(new VarInsnNode(ALOAD, varOffset + 1));
            // re-re-throw exception
            hook.add(new InsnNode(ATHROW));
            // } (end finally)

            if (isReturnNode) {
              // if we are a return then we need to insert after the previous instruction
              // methodVisitor.visitMethodInsn( INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false ); <- this is the result we are returning
              // we want to insert into this part to store/call finally before we return
              // methodVisitor.visitInsn( ARETURN ); <- this is node we are on (we removed this node at the start of this block)
              instructions.insert(previous, hook);
            } else if (isThrowNode) {
              // if we are a throw then we need to insert after the current instruction
              // methodVisitor.visitMethodInsn( INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false ); <- this is the line we are wrapping
              // methodVisitor.visitInsn( ATHROW );<- this is the node we are on
              // we want to insert here - as the throw will be 'caught' by the catch/finally we are inserting
              instructions.insert(node, hook);
            } else {
              // if we are a next line then we need to insert before the previous instruction
              // the previous instruction on next lines will be the 'label' for the next line

              // methodVisitor.visitFieldInsn( PUTFIELD, "com/nerdvision/agent/BPTestTarget", "name", "Ljava/lang/String;" );  - this is the line we are wrapping
              // Label label1 = new Label();
              // methodVisitor.visitLabel( label1 ); <- insert before this one
              // methodVisitor.visitLineNumber( 32, label1 ); <- this is the node we are on
              // methodVisitor.visitInsn( RETURN ); <- this is the next line
              instructions.insertBefore(previous, hook);
            }
            // remove the start, so we know this line is done.
            start = null;
            iBreakpoints.clear();
          }

          if (node.getType() == AbstractInsnNode.LABEL) {
            //noinspection DataFlowIssue
            LabelNode lab = (LabelNode) node;
            seenLabels.add(lab.getLabel());
          }

          if (node.getType() == AbstractInsnNode.LINE) {
            //noinspection DataFlowIssue
            ln = (LineNumberNode) node;
            // if we are a constructor and have not called super yet - the cache the line for later
            if (isConstructor && !hasCalledSuper) {
              constructorLine = ln;
              continue;
            }
            LOGGER.trace("visitMethod {} {} line number {}", classname, name, ln.line);
            // we remove them so only the first line has the breakpoint installed. Only the
            // first line will be hit as all subsequent will be disabled
            final List<TracePointConfig> thisLineBps = lineNos.remove((long) ln.line);
            if (thisLineBps != null) {
              // we track the breakpoints separate from the lineNos as we need to detect here what tracepoints should be installed,
              // but we might need to use them in forth coming instructions for line end etc
              iBreakpoints.clear();
              iBreakpoints.addAll(thisLineBps);
              // if we are not already mapped then check if we need to map it
              if (!isMapped.get()) {
                // do we need to wrap the method later
                final List<TracePointConfig> collect = thisLineBps.stream().filter(
                        tracePointConfig -> TracePointConfig.METHOD.equals(tracePointConfig.getArg(TracePointConfig.SPAN, String.class, null)))
                    .collect(Collectors.toList());
                if (!collect.isEmpty()) {
                  // we have a tracepoint on this line that is set to 'method' span type. So add the method to the mapped methods, then rename this method.
                  Visitor.this.mappedMethods.add(mappedMethod);
                  // track the mapped tracepoints, so we can pass them back later
                  final List<TracePointConfig> mappedTps = Visitor.this.methodBPs.computeIfAbsent(this.name,
                      k -> new ArrayList<>());
                  mappedTps.addAll(collect);

                  // rename the method and make it private synthetic $deep$..
                  this.name = determineNewMethodName(name);
                  this.access = replacedMethodAcc(TransformerUtils.isStatic(access));

                  // set is mapped so we do not check again
                  isMapped.set(true);
                }
              }
            }

            // check if we have any Bps for the constructor
            if (constructorLine != null) {
              // todo  how do we want to deal with method entry in constructors
              // add any bps for the constructor call into the list to be processed
              final List<TracePointConfig> constructorBps = lineNos.remove(
                  (long) constructorLine.line);
              if (constructorBps != null) {
                iBreakpoints.addAll(constructorBps);
              }
              constructorLine = null;
            }

            if (!iBreakpoints.isEmpty()) {
              start = ln.start;
              LOGGER.trace("visitMethod {} {} line number {}", classname, name, ln.line);
              // we insert the normal hook here, so we can capture the line data.
              // the try/catch/finally that is added later can be attached regardless of when this hook is
              // added as long as we have appropriate label instructions (which we add later)
              final InsnList hook = getAbstractInsnNodes(seenLabels, ln, iBreakpoints);

              changed = true;
              instructions.insert(ln, hook);
              if (isCf) {
                // if we are CF we do not support line capture, so we always clear the tracepoints
                // for non-cf the tracepoints are cleared after the try/finally is added (on the next instruction)
                iBreakpoints.clear();
              }

              LOGGER.debug("visitMethod {} {} patched @ {} {}", classname, name, ln.line, bps);
            }
          }
        }

        //  Use this to debug the raw byte code instruction changes in the even the visitors fail
        if (changed && INST_PRINTER_ENABLED) {
          for (AbstractInsnNode instruction : instructions) {
            System.out.println(InsnPrinter.prettyPrint(instruction));
          }
        }

        // now apply the modified instructions using the original class writer
        final MethodVisitor methodVisitor = Visitor.this.cv.visitMethod(this.access, this.name, desc, signature,
            exps);
        final JSRInlinerAdapter jsrInlinerAdapter = new JSRInlinerAdapter(methodVisitor, this.access, this.name,
            desc, signature,
            exps);

        this.accept(jsrInlinerAdapter);

        super.visitEnd();
      }


      private InsnList getAbstractInsnNodes(final Set<Label> seenLabels,
          final LineNumberNode ln,
          final List<TracePointConfig> breakpoints) {
        final InsnList hook = new InsnList();
        // try {

        // System.out.println("DEBUG BREAKPOINT class.method:line")
        if (DEBUG) {
          hook.add(
              new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
          hook.add(new LdcInsnNode("DEBUG BREAKPOINT " + classname + "." + name + ":" + ln.line));
          hook.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println",
              "(Ljava/lang/String;)V", false));

          hook.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(Thread.class), "dumpStack",
              Type.getMethodDescriptor(Type.VOID_TYPE), false));
        }

        // list = new ArrayList()
        hook.add(new TypeInsnNode(NEW, Type.getInternalName(ArrayList.class)));
        hook.add(new InsnNode(DUP));
        hook.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>",
            "()V", false));

        for (final TracePointConfig bp : breakpoints) {
          if (!bp.acceptStage(EStage.LINE_START)) {
            continue;
          }
          // list.add(bp.getId());
          hook.add(new InsnNode(DUP)); // we need a ptr to our map
          hook.add(new LdcInsnNode(bp.getId())); // bp id
          hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
              "(Ljava/lang/Object;)Z"));
          hook.add(new InsnNode(POP)); // dont care about return
        }

        // Put our location on stack for last call
        hook.add(new LdcInsnNode(filename));
        hook.add(new LdcInsnNode(ln.line));
        // Make a map of locals

        processLocalVariables(seenLabels, hook);

        // stack
        // -----
        // list of bp ids
        // filename
        // line no
        // map of vars

        // Call callback
        // Callback.callBack(list, filename, line, map)
        hook.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS),
            isCf ? "callBackCF" : "callBack",
            Type.getMethodDescriptor(Type.VOID_TYPE,
                Type.getType(List.class),
                Type.getType(String.class), Type.INT_TYPE, Type.getType(Map.class)),
            false));
        return hook;
      }


      private void processLocalVariables(final Set<Label> seenLabels,
          final InsnList hook) {
        // map = new HashMap()
        hook.add(new TypeInsnNode(NEW, Type.getInternalName(HashMap.class)));
        hook.add(new InsnNode(DUP));
        hook.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(HashMap.class), "<init>",
            "()V", false));

        // add this
        final boolean isStatic = isStatic(access);
        if (!isStatic) {
          // map.put("this", this)
          hook.add(new InsnNode(DUP)); // we need a ptr to our map
          hook.add(new LdcInsnNode("this")); // key
          hook.add(new VarInsnNode(ALOAD, 0)); // value
          // call map.put(name, value)
          hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(HashMap.class),
              "put",
              Type.getMethodDescriptor(Type.getType(Object.class),
                  Type.getType(Object.class), Type.getType(Object.class)),
              false));
          hook.add(new InsnNode(POP)); // dont care about return
        }

        // Have Map
        for (LocalVariableNode l : localVariables) {
          if (!isCf || CF_VARS.contains(l.name)) {
            // make sure we only process variables that are 'active' on the line that we
            // are triggered on
            if (seenLabels.contains(l.start.getLabel())
                && !seenLabels.contains(l.end.getLabel())) {
              // if the var is called 'this'
              // you cannot use the name 'this' for vars
              // we specifically capture 'this' above
              if (l.name.equals("this")) {
                continue;
              }
              // map.put('var name', var)
              LOGGER.debug("visitMethod {} {} Adding variable {}", classname, name, l.name);
              hook.add(new InsnNode(DUP)); // we need a ptr to our map
              hook.add(new LdcInsnNode(l.name)); // key
              hook.add(loadVariable(Type.getType(l.desc), l.index)); // value
              // call map.put(name, value)
              hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(HashMap.class),
                  "put",
                  Type.getMethodDescriptor(Type.getType(Object.class),
                      Type.getType(Object.class), Type.getType(Object.class)),
                  false));
              hook.add(new InsnNode(POP)); // dont care about return
            } else {
              LOGGER.debug("visitMethod {} {} Skipping variable {}", classname, name, l.name);
            }
          }
        }

        final Type[] argumentTypes = Type.getArgumentTypes(desc);
        // if we have params but no locals (except this) then add the params
        if (argumentTypes.length != 0 && noneOrJustThis(localVariables)) {
          for (int i = 0; i < argumentTypes.length; i++) {
            // map.put('param name', param);
            final Type argumentType = argumentTypes[i];
            final String pramName = "param" + i;
            LOGGER.debug("visitMethod {} {} Adding variable {}", classname, name, pramName);
            hook.add(new InsnNode(DUP)); // we need a ptr to our map
            hook.add(new LdcInsnNode(pramName)); // key
            hook.add(loadVariable(argumentType, i + 1)); // value
            // call map.put(name, value)
            hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(HashMap.class),
                "put",
                Type.getMethodDescriptor(Type.getType(Object.class),
                    Type.getType(Object.class), Type.getType(Object.class)),
                false));
            hook.add(new InsnNode(POP)); // dont care about return
          }
        }
      }
    };
  }

  @Override
  public void visitEnd() {
    for (MappedMethod mappedMethod : mappedMethods) {
      if (Type.getReturnType(mappedMethod.desc) == Type.VOID_TYPE) {
        createMappedVoidMethod(mappedMethod);
      } else {
        createMappedReturnMethod(mappedMethod);
      }
      changed = true;
    }

    super.visitEnd();
  }

  private void createMappedReturnMethod(final MappedMethod mappedMethod) {

    final List<TracePointConfig> breakpoints = this.methodBPs.get(mappedMethod.name);

    final MethodVisitor methodVisitor = this.cv.visitMethod(mappedMethod.access, mappedMethod.name, mappedMethod.desc, mappedMethod.sign,
        mappedMethod.excp);
    final Type[] argumentTypes = Type.getArgumentTypes(mappedMethod.desc);
    final int offset = argumentTypes.length;
    final Type returnType = Type.getReturnType(mappedMethod.desc);

    // setup try/catch label blocks
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
    Label label3 = new Label();
    methodVisitor.visitTryCatchBlock(label0, label1, label3, null);
    Label label4 = new Label();
    methodVisitor.visitTryCatchBlock(label2, label4, label3, null);

    Label label5 = new Label();
    methodVisitor.visitLabel(label5);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(70 + TransformerUtils.LINE_OFFSET, label5);
    }

    //methodName = "intTemplate()I"
    methodVisitor.visitLdcInsn(mappedMethod.name + mappedMethod.desc);
    // filename = "MockMixinTemplate.java"
    methodVisitor.visitLdcInsn(filename);
    // lineNo = 79
    methodVisitor.visitLdcInsn(mappedMethod.line);
    // list = new ArrayList();
    methodVisitor.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false);

    final StringBuilder spanOnlyIds = new StringBuilder();
    for (final TracePointConfig bp : breakpoints) {
      if (!bp.acceptStage(EStage.METHOD_START)) {
        if (TracePointConfig.METHOD.equals(bp.getArg(TracePointConfig.SPAN, String.class, null))) {
          spanOnlyIds.append(bp.getId()).append(",");
        }
        continue;
      }
      // list.add(bp.getId());
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bp.getId());
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
          "(Ljava/lang/Object;)Z", false);
      methodVisitor.visitInsn(POP); // dont care about return
    }

    // Make a map of locals
    // vars = new HashMap()
    processMethodArguments(mappedMethod.access, mappedMethod.vars, methodVisitor); // attach ids of tracepoints that just want a span crated
    if (spanOnlyIds.length() > 0) {
      spanOnlyIds.deleteCharAt(spanOnlyIds.length() - 1);
    }
    methodVisitor.visitLdcInsn(spanOnlyIds.toString());

    // Callback.methodEntry(mehodName, filename, lineNo, list, vars);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodEntry",
        Type.getMethodDescriptor(Type.VOID_TYPE,
            Type.getType(String.class),
            Type.getType(String.class),
            Type.getType(int.class),
            Type.getType(List.class),
            Type.getType(Map.class),
            Type.getType(String.class)), false);

    // try {
    methodVisitor.visitLabel(label0);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(72 + TransformerUtils.LINE_OFFSET, label0);
    }
    // i = this.$deep$intTemplate()
    methodVisitor.visitVarInsn(ALOAD, 0); // load all the parameters
    for (int i = 0; i < argumentTypes.length; i++) {
      final Type argumentType = argumentTypes[i];
      methodVisitor.visitVarInsn(argumentType.getOpcode(ILOAD), i + 1);
    }
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, this.classname, determineNewMethodName(mappedMethod.name), mappedMethod.desc,
        false);
    methodVisitor.visitVarInsn(returnType.getOpcode(ISTORE), 1 + offset);

    Label label6 = new Label();
    methodVisitor.visitLabel(label6);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(73 + TransformerUtils.LINE_OFFSET, label6);
    }
    // ret = new Integer(i);
    final InsnList abstractInsnNodes = loadVariable(returnType, 1 + offset);
    appendToVisitor(methodVisitor, abstractInsnNodes);

    // Callback.methodRet(ret);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodRet",
        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class)), false);
    // -- start good finally (ie the code in the finally block is run here if there is no exception)
    Label label7 = new Label();
    methodVisitor.visitLabel(label7);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(74 + TransformerUtils.LINE_OFFSET, label7);
    }
    // load and store ret
    // ret2 = ret;
    methodVisitor.visitVarInsn(returnType.getOpcode(ILOAD), 1 + offset);
    methodVisitor.visitVarInsn(returnType.getOpcode(ISTORE), 2 + offset);
    methodVisitor.visitLabel(label1);

    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(79 + TransformerUtils.LINE_OFFSET, label1);
    }

    //methodName = "intTemplate()I"
    methodVisitor.visitLdcInsn(mappedMethod.name + mappedMethod.desc);
    // filename = "MockMixinTemplate.java"
    methodVisitor.visitLdcInsn(filename);
    // lineNo = 79
    methodVisitor.visitLdcInsn(mappedMethod.line);
    // list = new ArrayList();
    methodVisitor.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false);

    for (final TracePointConfig bp : breakpoints) {
      if (!bp.acceptStage(EStage.METHOD_END)) {
        continue;
      }
      // list.add(bp.getId());
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bp.getId());
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
          "(Ljava/lang/Object;)Z", false);
      methodVisitor.visitInsn(POP); // dont care about return
    }

    // Make a map of locals
    // vars = new HashMap()
    processMethodArguments(mappedMethod.access, mappedMethod.vars, methodVisitor);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodEnd",
        Type.getMethodDescriptor(Type.VOID_TYPE,
            Type.getType(String.class),
            Type.getType(String.class),
            Type.getType(int.class),
            Type.getType(List.class),
            Type.getType(Map.class)), false);

    // reload ret and return
    Label label8 = new Label();
    methodVisitor.visitLabel(label8);

    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(74 + TransformerUtils.LINE_OFFSET, label8);
    }
    // return ret2;
    methodVisitor.visitVarInsn(returnType.getOpcode(ILOAD), 2 + offset);
    methodVisitor.visitInsn(returnType.getOpcode(IRETURN));
    methodVisitor.visitLabel(label2);
    // catch block
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(75 + TransformerUtils.LINE_OFFSET, label2);
    }
    // throwable = thrownException (store thrown exception into slot 1)
    methodVisitor.visitVarInsn(ASTORE, 1 + offset);
    Label label9 = new Label();
    methodVisitor.visitLabel(label9);

    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(76 + TransformerUtils.LINE_OFFSET, label9);
    }
    // t = throwable
    methodVisitor.visitVarInsn(ALOAD, 1 + offset);
    // Callback.methodException(t)
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodException",
        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Throwable.class)), false);

    Label label10 = new Label();
    methodVisitor.visitLabel(label10);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(77 + TransformerUtils.LINE_OFFSET, label10);
    }
    // throw t;
    methodVisitor.visitVarInsn(ALOAD, 1 + offset);
    methodVisitor.visitInsn(ATHROW);

    // -- start bad finally (ie the finally block when an exception was thrown)
    methodVisitor.visitLabel(label3);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(79 + TransformerUtils.LINE_OFFSET, label3);
    }
    // store thrown
    // t2 = t;
    methodVisitor.visitVarInsn(ASTORE, 3 + offset);
    methodVisitor.visitLabel(label4);

    //methodName = "intTemplate()I"
    methodVisitor.visitLdcInsn(mappedMethod.name + mappedMethod.desc);
    // filename = "MockMixinTemplate.java"
    methodVisitor.visitLdcInsn(filename);
    // lineNo = 79
    methodVisitor.visitLdcInsn(mappedMethod.line);
    // list = new ArrayList();
    methodVisitor.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false);

    for (final TracePointConfig bp : breakpoints) {
      if (!bp.acceptStage(EStage.METHOD_END)) {
        continue;
      }
      // list.add(bp.getId());
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bp.getId());
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
          "(Ljava/lang/Object;)Z", false);
      methodVisitor.visitInsn(POP); // dont care about return
    }

    // Make a map of locals
    // vars = new HashMap()
    processMethodArguments(mappedMethod.access, mappedMethod.vars, methodVisitor);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodEnd",
        Type.getMethodDescriptor(Type.VOID_TYPE,
            Type.getType(String.class),
            Type.getType(String.class),
            Type.getType(int.class),
            Type.getType(List.class),
            Type.getType(Map.class)), false);
    Label label11 = new Label();
    methodVisitor.visitLabel(label11);

    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(80 + TransformerUtils.LINE_OFFSET, label11);
    }
    // throw t2;
    methodVisitor.visitVarInsn(ALOAD, 3 + offset);
    methodVisitor.visitInsn(ATHROW);
    Label label12 = new Label();
    methodVisitor.visitLabel(label12);

    // no need to visit local vars as this code is synthetic we won't be debugging it anyway
    // the max/frames should be calculated for us
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }

  private void processMethodArguments(final int access, final List<MappedMethod.MappedVar> vars, final MethodVisitor methodVisitor) {
    // map = new HashMap()
    methodVisitor.visitTypeInsn(NEW, Type.getInternalName(HashMap.class));
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(HashMap.class), "<init>",
        "()V", false);

    // add this
    final boolean isStatic = isStatic(access);
    if (!isStatic) {
      // map.put("this", this)
      methodVisitor.visitInsn(DUP); // we need a ptr to our map
      methodVisitor.visitLdcInsn("this"); // key
      methodVisitor.visitVarInsn(ALOAD, 0); // value

      // call map.put(name, value)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(HashMap.class),
          "put",
          Type.getMethodDescriptor(Type.getType(Object.class),
              Type.getType(Object.class), Type.getType(Object.class)),
          false);
      methodVisitor.visitInsn(POP); // dont care about return
    }

    for (MappedVar var : vars) {

      // map.put('param name', param);
      final Type argumentType = Type.getType(var.desc);
      final String pramName = var.name;

      methodVisitor.visitInsn(DUP); // we need a ptr to our map
      methodVisitor.visitLdcInsn(pramName); // key
      // use the loadVariable method to ensure boxing
      final InsnList abstractInsnNodes = loadVariable(argumentType, var.index);
      // now convert to methodvisitor calls
      appendToVisitor(methodVisitor, abstractInsnNodes);
      // call map.put(name, value)
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(HashMap.class),
          "put",
          Type.getMethodDescriptor(Type.getType(Object.class),
              Type.getType(Object.class), Type.getType(Object.class)),
          false);
      methodVisitor.visitInsn(POP); // dont care about return
    }
  }

  private static void appendToVisitor(final MethodVisitor methodVisitor, final InsnList abstractInsnNodes) {
    for (AbstractInsnNode abstractInsnNode : abstractInsnNodes) {
      if (abstractInsnNode instanceof VarInsnNode) {
        methodVisitor.visitVarInsn(abstractInsnNode.getOpcode(), ((VarInsnNode) abstractInsnNode).var);
      } else if (abstractInsnNode instanceof MethodInsnNode) {
        final MethodInsnNode methodInstNode = (MethodInsnNode) abstractInsnNode;
        methodVisitor.visitMethodInsn(abstractInsnNode.getOpcode(), methodInstNode.owner, methodInstNode.name, methodInstNode.desc,
            methodInstNode.itf);
      } else if (abstractInsnNode instanceof LdcInsnNode) {
        methodVisitor.visitLdcInsn(((LdcInsnNode) abstractInsnNode).cst);
      }
    }
  }

  private void createMappedVoidMethod(final MappedMethod mappedMethod) {
    final List<TracePointConfig> breakpoints = this.methodBPs.get(mappedMethod.name);
    final MethodVisitor methodVisitor = this.cv.visitMethod(mappedMethod.access, mappedMethod.name, mappedMethod.desc, mappedMethod.sign,
        mappedMethod.excp);
    final Type[] argumentTypes = Type.getArgumentTypes(mappedMethod.desc);
    final int offset = argumentTypes.length;

    methodVisitor.visitCode();
    // try-catch labels
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
    Label label3 = new Label();
    methodVisitor.visitTryCatchBlock(label0, label1, label3, null);
    Label label4 = new Label();
    methodVisitor.visitTryCatchBlock(label2, label4, label3, null);
    Label label5 = new Label();
    methodVisitor.visitLabel(label5);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(38 + TransformerUtils.LINE_OFFSET, label5);
    }

    //methodName = "intTemplate()I"
    methodVisitor.visitLdcInsn(mappedMethod.name + mappedMethod.desc);
    // filename = "MockMixinTemplate.java"
    methodVisitor.visitLdcInsn(filename);
    // lineNo = 79
    methodVisitor.visitLdcInsn(mappedMethod.line);
    // list = new ArrayList();
    methodVisitor.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false);

    final StringBuilder spanOnlyIds = new StringBuilder();
    for (final TracePointConfig bp : breakpoints) {
      if (!bp.acceptStage(EStage.METHOD_START)) {
        if (TracePointConfig.METHOD.equals(bp.getArg(TracePointConfig.SPAN, String.class, null))) {
          spanOnlyIds.append(bp.getId()).append(",");
        }
        continue;
      }
      // list.add(bp.getId());
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bp.getId());
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
          "(Ljava/lang/Object;)Z", false);
      methodVisitor.visitInsn(POP); // dont care about return
    }

    // Make a map of locals
    // vars = new HashMap()
    processMethodArguments(mappedMethod.access, mappedMethod.vars, methodVisitor);
    // attach ids of tracepoints that just want a span crated
    if (spanOnlyIds.length() > 0) {
      spanOnlyIds.deleteCharAt(spanOnlyIds.length() - 1);
    }
    methodVisitor.visitLdcInsn(spanOnlyIds.toString());

    // Callback.methodEntry(mehodName, filename, lineNo, list, vars);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodEntry",
        Type.getMethodDescriptor(Type.VOID_TYPE,
            Type.getType(String.class),
            Type.getType(String.class),
            Type.getType(int.class),
            Type.getType(List.class),
            Type.getType(Map.class),
            Type.getType(String.class)), false);

    methodVisitor.visitLabel(label0);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(40 + TransformerUtils.LINE_OFFSET, label0);
    }
    // this.$deep$voidTemplate();
    methodVisitor.visitVarInsn(ALOAD, 0); // load all the parameters
    for (int i = 0; i < argumentTypes.length; i++) {
      final Type argumentType = argumentTypes[i];
      methodVisitor.visitVarInsn(argumentType.getOpcode(ILOAD), i + 1);
    }
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, this.classname, determineNewMethodName(mappedMethod.name), mappedMethod.desc,
        false);
    // -- start good finally (ie the code in the finally block is run here if there is no exception)
    methodVisitor.visitLabel(label1);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(45 + TransformerUtils.LINE_OFFSET, label1);
    }

    //methodName = "intTemplate()I"
    methodVisitor.visitLdcInsn(mappedMethod.name + mappedMethod.desc);
    // filename = "MockMixinTemplate.java"
    methodVisitor.visitLdcInsn(filename);
    // lineNo = 79
    methodVisitor.visitLdcInsn(mappedMethod.line);
    // list = new ArrayList();
    methodVisitor.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false);

    for (final TracePointConfig bp : breakpoints) {
      if (!bp.acceptStage(EStage.METHOD_END)) {
        continue;
      }
      // list.add(bp.getId());
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bp.getId());
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
          "(Ljava/lang/Object;)Z", false);
      methodVisitor.visitInsn(POP); // dont care about return
    }

    // Make a map of locals
    // vars = new HashMap()
    processMethodArguments(mappedMethod.access, mappedMethod.vars, methodVisitor);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodEnd",
        Type.getMethodDescriptor(Type.VOID_TYPE,
            Type.getType(String.class),
            Type.getType(String.class),
            Type.getType(int.class),
            Type.getType(List.class),
            Type.getType(Map.class)), false);

    // catch(Throwable t)
    Label label6 = new Label();
    methodVisitor.visitLabel(label6);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(46 + TransformerUtils.LINE_OFFSET, label6);
    }
    Label label7 = new Label();
    methodVisitor.visitJumpInsn(GOTO, label7);
    methodVisitor.visitLabel(label2);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(41 + TransformerUtils.LINE_OFFSET, label2);
    }
    // throwable = thrownException (store thrown exception into slot 1)
    methodVisitor.visitVarInsn(ASTORE, 1 + offset);
    Label label8 = new Label();
    methodVisitor.visitLabel(label8);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(42 + TransformerUtils.LINE_OFFSET, label8);
    }

    // Callback.methodException(throwable)
    methodVisitor.visitVarInsn(ALOAD, 1 + offset);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodException",
        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Throwable.class)), false);

    Label label9 = new Label();
    methodVisitor.visitLabel(label9);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(43 + TransformerUtils.LINE_OFFSET, label9);
    }
    // throw throwable
    methodVisitor.visitVarInsn(ALOAD, 1 + offset);
    methodVisitor.visitInsn(ATHROW);

    methodVisitor.visitLabel(label3);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(45 + TransformerUtils.LINE_OFFSET, label3);
    }

    // -- start bad finally (ie the code in the finally block is run here if there is an exception)
    // t2 = throwable;
    methodVisitor.visitVarInsn(ASTORE, 2 + offset);
    methodVisitor.visitLabel(label4);

    //methodName = "intTemplate()I"
    methodVisitor.visitLdcInsn(mappedMethod.name + mappedMethod.desc);
    // filename = "MockMixinTemplate.java"
    methodVisitor.visitLdcInsn(filename);
    // lineNo = 79
    methodVisitor.visitLdcInsn(mappedMethod.line);
    // list = new ArrayList();
    methodVisitor.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false);

    for (final TracePointConfig bp : breakpoints) {
      if (!bp.acceptStage(EStage.METHOD_END)) {
        continue;
      }
      // list.add(bp.getId());
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bp.getId());
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ArrayList.class), "add",
          "(Ljava/lang/Object;)Z", false);
      methodVisitor.visitInsn(POP); // dont care about return
    }

    // Make a map of locals
    // vars = new HashMap()
    processMethodArguments(mappedMethod.access, mappedMethod.vars, methodVisitor);
    // Callback.methodEnd(methodName, filename, lineNo, list, vars);
    methodVisitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CALLBACK_CLASS), "methodEnd",
        Type.getMethodDescriptor(Type.VOID_TYPE,
            Type.getType(String.class),
            Type.getType(String.class),
            Type.getType(int.class),
            Type.getType(List.class),
            Type.getType(Map.class)), false);

    Label label10 = new Label();
    methodVisitor.visitLabel(label10);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(46 + TransformerUtils.LINE_OFFSET, label10);
    }
    // throw t2;
    methodVisitor.visitVarInsn(ALOAD, 2 + offset);
    methodVisitor.visitInsn(ATHROW);
    methodVisitor.visitLabel(label7);
    if (TransformerUtils.ALLOW_LINE_NUMBERS) {
      methodVisitor.visitLineNumber(47 + TransformerUtils.LINE_OFFSET, label7);
    }
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    methodVisitor.visitInsn(RETURN);
    Label label11 = new Label();
    methodVisitor.visitLabel(label11);

    // no need to visit local vars as this code is synthetic we won't be debugging it anyway
    // the max/frames should be calculated for us
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }


  /**
   * This is used in a comment on line 509 and is left in place for debugging.
   */
  @SuppressWarnings("unused")
  public static class InsnPrinter {

    private static final Printer printer = new Textifier();
    private static final TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);


    static String prettyPrint(AbstractInsnNode insnNode) {
      insnNode.accept(methodPrinter);
      StringWriter sw = new StringWriter();
      printer.print(new PrintWriter(sw));
      printer.getText().clear();
      return sw.toString();
    }
  }

  static class MappedMethod {


    final int access;
    final String name;
    final String desc;
    final String sign;
    final String[] excp;
    int line;
    final List<MappedVar> vars;

    public MappedMethod(final int access, final String name, final String desc, final String sign, final String[] excp, final int line) {
      this.access = access;
      this.name = name;
      this.desc = desc;
      this.sign = sign;
      this.excp = excp;
      this.line = line;
      this.vars = new ArrayList<>();
    }

    void acceptLine(final int line) {
      this.line = Math.max(this.line, line);
    }

    void acceptVariable(final String name, final String desc, final int index) {
      this.vars.add(new MappedVar(name, desc, index));
    }

    static class MappedVar {

      final String name;
      final String desc;
      final int index;

      public MappedVar(final String name, final String desc, final int index) {
        this.name = name;
        this.desc = desc;
        this.index = index;
      }
    }
  }
}
