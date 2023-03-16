package io.github.sgpublic.xxpref.core

import com.squareup.javapoet.ClassName
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.List
import io.github.sgpublic.xxpref.XXPrefProcessor.Companion.mTrees
import io.github.sgpublic.xxpref.annotations.ExSharedPreference
import io.github.sgpublic.xxpref.annotations.ExValue
import io.github.sgpublic.xxpref.base.ListElementVisitor
import io.github.sgpublic.xxpref.jc.accept
import io.github.sgpublic.xxpref.jc.to
import io.github.sgpublic.xxpref.util.Logable
import io.github.sgpublic.xxpref.util.capitalize
import io.github.sgpublic.xxpref.util.supported
import io.github.sgpublic.xxpref.visitor.*
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class PreferenceCompiler private constructor(
    override val targetElement: TypeElement
): Logable, ListElementVisitor<JCTree, ExSharedPreference> {
    override val targetAnnotation: Class<out Annotation> = ExSharedPreference::class.java
    companion object {
        fun apply(env: RoundEnvironment) {
            for (element: Element in env.getElementsAnnotatedWith(ExSharedPreference::class.java)) {
                if (element !is TypeElement) {
                    continue
                }
                element.accept(
                    PreferenceCompiler(element),
                    element.getAnnotation(ExSharedPreference::class.java)
                ).to(mTrees.getTree(element))
            }
        }
    }

    override fun visitType(element: TypeElement, param: ExSharedPreference): List<JCTree> {
        // import android.content.SharedPreferences;
        element.accept(
            ImportDefVisitor, ImportDefVisitor.ClassPkgDef(
            "android.content.SharedPreferences", "SharedPreferences"
        )).to(EditorDefVisitor.mTrees.getTree(element))
        // import io.github.sgpublic.xxpref.SpEditor;
        element.accept(
            ImportDefVisitor, ImportDefVisitor.ClassPkgDef(
            "io.github.sgpublic.xxpref.SpEditor", "SpEditor"
        )).to(EditorDefVisitor.mTrees.getTree(element))
        // import io.github.sgpublic.xxpref.ExPreference;
        element.accept(
            ImportDefVisitor, ImportDefVisitor.ClassPkgDef(
            "io.github.sgpublic.xxpref.ExPreference", "ExPreference.Reference"
        )).to(EditorDefVisitor.mTrees.getTree(element))
        // import io.github.sgpublic.xxpref.ExConverters;
        element.accept(
            ImportDefVisitor, ImportDefVisitor.ClassPkgDef(
            "io.github.sgpublic.xxpref.ExConverters", "ExConverters"
        )).to(EditorDefVisitor.mTrees.getTree(element))

        val list = List.nil<JCTree>()

        element.accept(SpRefDefVisitor, param).to(list)
        val editor = element.accept(EditorDefVisitor).to(list)

        for (field: Element in targetElement.enclosedElements) {
            if (field !is VariableElement || field.modifiers.contains(Modifier.FINAL)) {
                continue
            }

            val origType = ClassName.get(element.asType())
            if (!origType.supported()) {
                // import OriginT;
                origType as ClassName
                element.accept(ImportDefVisitor, ImportDefVisitor.ClassPkgDef(
                    origType.packageName(), origType.simpleName()
                ))
            }

            val exValue = (field.getAnnotation(ExValue::class.java) ?: continue).let {
                return@let ExValue(it.defVal, it.key.capitalize())
            }

            field.accept(GetterDefVisitor, exValue).to(list)
            field.accept(SetterDefVisitor, exValue).to(list)
            field.accept(EditorSetterDefVisitor, exValue).to(editor)
        }

        return list
    }
}