package org.apache.lucene.index;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

/** 
 * 线程索引链1实现：对索引域进行处理
 * This is a DocFieldConsumer that inverts each field,
 *  separately, from a Document, and accepts a
 *  InvertedTermsConsumer to process those terms. */

final class DocInverterPerThread extends DocFieldConsumerPerThread {
  final DocInverter docInverter;
  final InvertedDocConsumerPerThread consumer;
  final InvertedDocEndConsumerPerThread endConsumer;
  final SingleTokenAttributeSource singleToken = new SingleTokenAttributeSource();
  
/**
 * 存储不需要分词Token的各种属性信息，为索引做准备
 */
static class SingleTokenAttributeSource extends AttributeSource {
    final TermAttribute termAttribute;
    final OffsetAttribute offsetAttribute;
    
    private SingleTokenAttributeSource() {
      termAttribute = addAttribute(TermAttribute.class);
      offsetAttribute = addAttribute(OffsetAttribute.class);
    }
    
    public void reinit(String stringValue, int startOffset,  int endOffset) {
      termAttribute.setTermBuffer(stringValue);
      offsetAttribute.setOffset(startOffset, endOffset);
    }
  }
  
  final DocumentsWriter.DocState docState;

  final FieldInvertState fieldState = new FieldInvertState();

  // Used to read a string value for a field
  final ReusableStringReader stringReader = new ReusableStringReader();

  public DocInverterPerThread(DocFieldProcessorPerThread docFieldProcessorPerThread, DocInverter docInverter) {
    this.docInverter = docInverter;
    docState = docFieldProcessorPerThread.docState;
    consumer = docInverter.consumer.addThread(this);
    endConsumer = docInverter.endConsumer.addThread(this);
  }

  @Override
  public void startDocument() throws IOException {
    consumer.startDocument();
    endConsumer.startDocument();
  }

  @Override
  public DocumentsWriter.DocWriter finishDocument() throws IOException {
    // TODO: allow endConsumer.finishDocument to also return
    // a DocWriter
    endConsumer.finishDocument();
    return consumer.finishDocument();
  }

  @Override
  void abort() {
    try {
      consumer.abort();
    } finally {
      endConsumer.abort();
    }
  }

  @Override
  public DocFieldConsumerPerField addField(FieldInfo fi) {
    return new DocInverterPerField(this, fi);
  }
}