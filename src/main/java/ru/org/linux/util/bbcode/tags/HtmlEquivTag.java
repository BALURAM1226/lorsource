/*
 * Copyright (c) 2005-2006, Luke Plant
 * All rights reserved.
 * E-mail: <L.Plant.98@cantab.net>
 * Web: http://lukeplant.me.uk/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *
 *      * The name of Luke Plant may not be used to endorse or promote
 *        products derived from this software without specific prior
 *        written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Rewrite with Java language and modified for lorsource by Ildar Hizbulin 2011
 * E-mail: <hizel@vyborg.ru>
 */

package ru.org.linux.util.bbcode.tags;

import com.google.common.collect.ImmutableMap;
import ru.org.linux.util.bbcode.Parser;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.bbcode.nodes.Node;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/30/11
 * Time: 10:40 AM
 */
public class HtmlEquivTag extends Tag {
  private String htmlEquiv;
  private Map<String, String> attributes = ImmutableMap.of();

  public HtmlEquivTag(String name, Set<String> allowedChildren, String implicitTag, ParserParameters parserParameters) {
    super(name, allowedChildren, implicitTag, parserParameters);
  }

  public void setHtmlEquiv(String htmlEquiv) {
    this.htmlEquiv = htmlEquiv;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  @Override
  public String renderNodeXhtml(Node node) {
    StringBuilder opening = new StringBuilder(htmlEquiv);
    StringBuilder ret = new StringBuilder();

    if (!attributes.isEmpty()) {
      opening.append(' ');

      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        opening.append(entry.getKey());
        opening.append('=');
        opening.append(Parser.escape(entry.getValue()));
        opening.append(' ');
      }
    }

    if (htmlEquiv.isEmpty()) {
      ret.append(node.renderChildrenXHtml());
    } else {
      if (selfClosing) {
        ret.append('<').append(opening).append('>'); // для xhtml по идее />
      } else {
        if (node.lengthChildren() > 0) {
          ret.append('<').append(opening).append('>');
          ret.append(node.renderChildrenXHtml());
          ret.append("</").append(htmlEquiv).append('>');
        }
      }
    }
    return ret.toString();
  }

  @Override
  public String renderNodeBBCode(Node node) {
    if ("div".equals(name)) {
      return node.renderChildrenBBCode();
    } else {
      return super.renderNodeBBCode(node);
    }
  }
}
