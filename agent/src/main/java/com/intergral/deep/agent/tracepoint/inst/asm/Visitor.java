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
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

import com.google.common.collect.Sets;
import com.intergral.deep.agent.types.TracePointConfig;
import java.com.intergral.deep.ProxyCallback;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
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

@SuppressWarnings({"DuplicatedCode", "CommentedOutCode"})
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
  private static final boolean DEBUG = false;

  private final Collection<TracePointConfig> bps;
  private final boolean isCf;
  private final Map<Long, List<TracePointConfig>> lineNos;

  private String classname;
  private String superName;
  private String filename;

  private boolean changed = false;

  public static final Class<?> CALLBACK_CLASS;

  static {
    // this is here to make the tests easier.
    // we cannot use java. classes in the tests without screwing with the class loaders
    // so in the tests we use the 'nv.callback.class' which is the CallBack.class
    // at runtime we use the ProxyCallback.class so we can bypass the osgi classloading restrictions
    final String property = System.getProperty("nv.callback.class");
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

  public Visitor(final ClassVisitor v, final Collection<TracePointConfig> bps, final boolean isCf) {
    super(ASM8, v);
    this.bps = bps;
    this.isCf = isCf;
    lineNos = new HashMap<>();
    for (final TracePointConfig bp : bps) {
      final long lineNo = bp.getLineNo();
      List<TracePointConfig> list = lineNos.get(lineNo);

      //noinspection Java8MapApi
      if (list == null) {
        list = new ArrayList<>();
        lineNos.put(lineNo, list);
      }
      list.add(bp);
    }
  }


  public boolean wasChanged() {
    return changed;
  }


  public String getFilename() {
    return filename;
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


  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exceptions) {
    LOGGER.debug("visitMethod {} {}", classname, name);
    final MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature,
        exceptions);
    final JSRInlinerAdapter jsrInlinerAdapter = new JSRInlinerAdapter(methodVisitor, access, name,
        desc, signature,
        exceptions);

    // MethodNode used to handle the maxes for us to make it simpler
    return new MethodNode(ASM7, access, name, desc, signature, exceptions) {

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
                case Opcodes.IRETURN:
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

            // set = new HashSet()
            hook.add(new TypeInsnNode(NEW, Type.getInternalName(HashSet.class)));
            hook.add(new InsnNode(DUP));
            hook.add(
                new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(HashSet.class), "<init>",
                    "()V", false));

            for (final TracePointConfig bp : iBreakpoints) {
              // set.add(bp.getId());
              hook.add(new InsnNode(DUP)); // we need a ptr to our map
              hook.add(new LdcInsnNode(bp.getId())); // bp id
              hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(HashSet.class), "add",
                  "(Ljava/lang/Object;)Z"));
              hook.add(new InsnNode(POP)); // dont care about return
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
                    Type.getType(Set.class),
                    Type.getType(Map.class)),
                false));
            if (isReturnNode) {
              // if we are not a void return the reload the var
              if (node.getOpcode() != RETURN) {
                // we have to load the return but we need to use the correct load code
                // we use the return code to find the correct load code
                final int opLoad;
                switch (node.getOpcode()) {
                  case Opcodes.IRETURN:
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
                startFinally)); // we start the finally before we end the catch for some reason
            // 'bad' finally {
            // store exception in next slot
            hook.add(new VarInsnNode(ASTORE, varOffset + 1));
            // finally {
            hook.add(new LabelNode(endCatch));

            // set = new HashSet()
            hook.add(new TypeInsnNode(NEW, Type.getInternalName(HashSet.class)));
            hook.add(new InsnNode(DUP));
            hook.add(
                new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(HashSet.class), "<init>",
                    "()V", false));

            for (final TracePointConfig bp : iBreakpoints) {
              // set.add(bp.getId());
              hook.add(new InsnNode(DUP)); // we need a ptr to our map
              hook.add(new LdcInsnNode(bp.getId())); // bp id
              hook.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(HashSet.class), "add",
                  "(Ljava/lang/Object;)Z"));
              hook.add(new InsnNode(POP)); // dont care about return
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
                    Type.getType(Set.class),
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
              // if we are a next line then we need to insert before the previous
              // the previous on next lines will be the 'label' for the next line

              // methodVisitor.visitFieldInsn( PUTFIELD, "com/nerdvision/agent/BPTestTarget", "name", "Ljava/lang/String;" );  - this is the line we are wrapping
              // Label label1 = new Label();
              // methodVisitor.visitLabel( label1 ); <- insert before this one
              // methodVisitor.visitLineNumber( 32, label1 ); <- this it the node we are on
              // methodVisitor.visitInsn( RETURN ); <- this is the next line
              instructions.insertBefore(previous, hook);
            }
            // remove the start so we know this line is done.
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
            LineNumberNode ln = (LineNumberNode) node;
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
              iBreakpoints.clear();
              iBreakpoints.addAll(thisLineBps);
            }

            // check if we have any Bps for the constructor
            if (constructorLine != null) {
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
              final InsnList hook = getAbstractInsnNodes(seenLabels, ln, iBreakpoints);

              changed = true;
              instructions.insert(ln, hook);

              LOGGER.debug("visitMethod {} {} patched @ {} {}", classname, name, ln.line, bps);
            }
          }
        }

        //  Use this to debug the raw byte code instruction changes in the even the visitors fail
        //  if(changed)
        //  {
        //      for( AbstractInsnNode instruction : instructions )
        //      {
        //          System.out.println(InsnPrinter.prettyPrint( instruction ));
        //      }
        //  }

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
      case Opcodes.IRETURN:
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


  /**
   * Converts primatives to objects so we can put them in the map
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
   * This is used in a comment on line 509 and is left in place for debugging
   */
  @SuppressWarnings("unused")
  public static class InsnPrinter {

    private static final Printer printer = new Textifier();
    private static final TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);


    public static String prettyPrint(AbstractInsnNode insnNode) {
      insnNode.accept(methodPrinter);
      StringWriter sw = new StringWriter();
      printer.print(new PrintWriter(sw));
      printer.getText().clear();
      return sw.toString();
    }
  }
}
