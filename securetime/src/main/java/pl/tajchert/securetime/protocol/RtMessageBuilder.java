/*
 * Copyright (c) 2017 int08h LLC. All rights reserved.
 *
 * int08h LLC licenses Nearenough (the "Software") to you under the Apache License, version 2.0
 * (the "License"); you may not use this Software except in compliance with the License. You may
 * obtain a copy of the License from the LICENSE file included with the Software or at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.tajchert.securetime.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import static pl.tajchert.securetime.protocol.RtConstants.MIN_REQUEST_LENGTH;
import static pl.tajchert.securetime.util.Preconditions.checkArgument;
import static pl.tajchert.securetime.util.Preconditions.checkNotNull;

public final class RtMessageBuilder {

    private final Map<pl.tajchert.securetime.protocol.RtTag, byte[]> map = new TreeMap<>(
            Comparator.comparing(pl.tajchert.securetime.protocol.RtTag::valueLE, Integer::compareUnsigned)
    );

    private ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
    private boolean shouldAddPadding = false;

    public RtMessageBuilder add(pl.tajchert.securetime.protocol.RtTag tag, byte[] value) {
        checkNotNull(tag, "tag must be non-null");

        map.put(tag, value);
        return this;
    }

    public RtMessageBuilder add(pl.tajchert.securetime.protocol.RtTag tag, ByteBuf value) {
        checkNotNull(tag, "tag must be non-null");
        checkNotNull(value, "value must be non-null");

        byte[] bytes = new byte[value.readableBytes()];
        value.readBytes(bytes);
        map.put(tag, bytes);

        return this;
    }

    public RtMessageBuilder add(pl.tajchert.securetime.protocol.RtTag tag, RtMessage msg) {
        checkNotNull(tag, "tag must be non-null");
        checkNotNull(msg, "msg must be non-null");

        ByteBuf encoded = pl.tajchert.securetime.protocol.RtWire.toWire(msg, allocator);
        return add(tag, encoded);
    }

    public RtMessageBuilder addPadding(boolean shouldPad) {
        this.shouldAddPadding = shouldPad;
        return this;
    }

    public RtMessageBuilder allocator(ByteBufAllocator allocator) {
        checkNotNull(allocator, "allocator must be non-null");

        this.allocator = allocator;
        return this;
    }

    public RtMessage build() {
        checkArgument(!map.isEmpty(), "Cannot build an empty RtMessage");

        int encodedSize = pl.tajchert.securetime.protocol.RtWire.computeEncodedSize(map);

        if (encodedSize < MIN_REQUEST_LENGTH && shouldAddPadding) {
            // Additional bytes added to message size for PAD tag (4) and its offset field (4)
            int padOverhead = 8;
            // The overhead bytes alone may be sufficient to reach the minimum size; it's possible
            // to end up with a zero-length pad value
            int paddingBytes = Math.max(0, (MIN_REQUEST_LENGTH - encodedSize - padOverhead));
            byte[] padding = new byte[paddingBytes];
            map.put(pl.tajchert.securetime.protocol.RtTag.PAD, padding);
        }

        return new RtMessage(this);
    }

    /*package*/ Map<pl.tajchert.securetime.protocol.RtTag, byte[]> mapping() {
        return map;
    }

}
