using Parsers.Common;
using System.Collections.Generic;

namespace Parsers.Common
{
    public class NodeManager {
        public List<object> nodes;
        public Reader reader;
        
        public NodeManager(Reader reader)
        {
            this.reader = reader;
            this.nodes = new List<object>();
        }
        
        public void addNode(object node, int start)
        {
            this.nodes.push(node);
        }
    }
}