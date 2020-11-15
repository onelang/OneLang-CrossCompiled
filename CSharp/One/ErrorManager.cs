using One.Ast;
using One;
using System.Collections.Generic;
using Utils;

namespace One
{
    public enum LogType { Info, Warning, Error }
    
    public class CompilationError {
        public string msg;
        public bool isWarning;
        public string transformerName;
        public IAstNode node;
        
        public CompilationError(string msg, bool isWarning, string transformerName, IAstNode node)
        {
            this.msg = msg;
            this.isWarning = isWarning;
            this.transformerName = transformerName;
            this.node = node;
        }
    }
    
    public class ErrorManager {
        public AstTransformer transformer;
        public IAstNode currentNode;
        public List<CompilationError> errors;
        public string lastContextInfo;
        
        public string location {
            get {
             {
                var t = this.transformer;
                
                var par = this.currentNode;
                while (par is Expression expr)
                    par = expr.parentNode;
                
                string location = null;
                if (par is Field field)
                    location = $"{field.parentInterface.parentFile.sourcePath.path} -> {field.parentInterface.name}::{field.name} (field)";
                else if (par is Property prop)
                    location = $"{prop.parentClass.parentFile.sourcePath.path} -> {prop.parentClass.name}::{prop.name} (property)";
                else if (par is Method meth)
                    location = $"{meth.parentInterface.parentFile.sourcePath.path} -> {meth.parentInterface.name}::{meth.name} (method)";
                else if (par is Constructor const_)
                    location = $"{const_.parentClass.parentFile.sourcePath.path} -> {const_.parentClass.name}::constructor";
                else if (par == null) { }
                else if (par is Statement) { }
                else { }
                
                if (location == null && t != null && t.currentFile != null) {
                    location = $"{t.currentFile.sourcePath.path}";
                    if (t.currentInterface != null) {
                        location += $" -> {t.currentInterface.name}";
                        if (t.currentMethod is Method meth2)
                            location += $"::{meth2.name}";
                        else if (t.currentMethod is Constructor)
                            location += $"::constructor";
                        else if (t.currentMethod is Lambda)
                            location += $"::<lambda>";
                        else if (t.currentMethod == null) { }
                        else { }
                    }
                }
                
                return location;
            }
            }
        }
        public string currentNodeRepr {
            get {
            
                return TSOverviewGenerator.preview.nodeRepr(this.currentNode);
            }
        }
        public string currentStatementRepr {
            get {
            
                return this.transformer.currentStatement == null ? "<null>" : TSOverviewGenerator.preview.stmt(this.transformer.currentStatement);
            }
        }
        
        public ErrorManager()
        {
            this.transformer = null;
            this.currentNode = null;
            this.errors = new List<CompilationError>();
        }
        
        public void resetContext(AstTransformer transformer = null)
        {
            this.transformer = transformer;
        }
        
        public void log(LogType type, string msg)
        {
            var t = this.transformer;
            var text = (t != null ? $"[{t.name}] " : "") + msg;
            
            if (this.currentNode != null)
                text += $"\n  Node: {this.currentNodeRepr}";
            
            var location = this.location;
            if (location != null)
                text += $"\n  Location: {location}";
            
            if (t != null && t.currentStatement != null)
                text += $"\n  Statement: {this.currentStatementRepr}";
            
            if (this.lastContextInfo != null)
                text += $"\n  Context: {this.lastContextInfo}";
            
            if (type == LogType.Info)
                console.log(text);
            else if (type == LogType.Warning)
                console.error($"[WARNING] {text}\n");
            else if (type == LogType.Error)
                console.error($"{text}\n");
            else { }
            
            if (type == LogType.Error || type == LogType.Warning)
                this.errors.push(new CompilationError(msg, type == LogType.Warning, t != null ? t.name : null, this.currentNode));
        }
        
        public void info(string msg)
        {
            this.log(LogType.Info, msg);
        }
        
        public void warn(string msg)
        {
            this.log(LogType.Warning, msg);
        }
        
        public void throw_(string msg)
        {
            this.log(LogType.Error, msg);
        }
    }
}