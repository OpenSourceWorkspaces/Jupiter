package org.jupiter.benchmark.serialization;

import io.netty.buffer.*;
import io.netty.util.internal.PlatformDependent;
import org.jupiter.common.util.Lists;
import org.jupiter.serialization.*;
import org.jupiter.transport.netty.alloc.AdaptiveOutputBufAllocator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SerializationBenchmark {

    /*
        Benchmark                                      Mode     Cnt    Score    Error   Units
        SerializationBenchmark.hessianByteBuffer      thrpt      10   32.483 ±  0.602  ops/ms
        SerializationBenchmark.hessianBytesArray      thrpt      10   29.710 ±  0.780  ops/ms
        SerializationBenchmark.javaByteBuffer         thrpt      10   10.691 ±  0.309  ops/ms
        SerializationBenchmark.javaBytesArray         thrpt      10   10.184 ±  0.111  ops/ms
        SerializationBenchmark.kryoByteBuffer         thrpt      10   43.679 ±  1.152  ops/ms
        SerializationBenchmark.kryoBytesArray         thrpt      10   48.685 ±  2.737  ops/ms
        SerializationBenchmark.protoStuffByteBuffer   thrpt      10  382.429 ± 19.064  ops/ms
        SerializationBenchmark.protoStuffBytesArray   thrpt      10  382.446 ± 16.843  ops/ms
        SerializationBenchmark.hessianByteBuffer       avgt      10    0.031 ±  0.001   ms/op
        SerializationBenchmark.hessianBytesArray       avgt      10    0.034 ±  0.001   ms/op
        SerializationBenchmark.javaByteBuffer          avgt      10    0.091 ±  0.001   ms/op
        SerializationBenchmark.javaBytesArray          avgt      10    0.098 ±  0.002   ms/op
        SerializationBenchmark.kryoByteBuffer          avgt      10    0.023 ±  0.001   ms/op
        SerializationBenchmark.kryoBytesArray          avgt      10    0.021 ±  0.001   ms/op
        SerializationBenchmark.protoStuffByteBuffer    avgt      10    0.003 ±  0.001   ms/op
        SerializationBenchmark.protoStuffBytesArray    avgt      10    0.003 ±  0.001   ms/op
        SerializationBenchmark.hessianByteBuffer     sample  162209   ≈ 10⁻⁵             s/op
        SerializationBenchmark.hessianBytesArray     sample  150403   ≈ 10⁻⁴             s/op
        SerializationBenchmark.javaByteBuffer        sample  112900   ≈ 10⁻⁴             s/op
        SerializationBenchmark.javaBytesArray        sample  101405   ≈ 10⁻⁴             s/op
        SerializationBenchmark.kryoByteBuffer        sample  108397   ≈ 10⁻⁵             s/op
        SerializationBenchmark.kryoBytesArray        sample  120872   ≈ 10⁻⁵             s/op
        SerializationBenchmark.protoStuffByteBuffer  sample  118416   ≈ 10⁻⁶             s/op
        SerializationBenchmark.protoStuffBytesArray  sample  119432   ≈ 10⁻⁶             s/op
        SerializationBenchmark.hessianByteBuffer         ss      10    0.001 ±  0.001    s/op
        SerializationBenchmark.hessianBytesArray         ss      10    0.001 ±  0.001    s/op
        SerializationBenchmark.javaByteBuffer            ss      10    0.002 ±  0.001    s/op
        SerializationBenchmark.javaBytesArray            ss      10    0.002 ±  0.001    s/op
        SerializationBenchmark.kryoByteBuffer            ss      10    0.001 ±  0.001    s/op
        SerializationBenchmark.kryoBytesArray            ss      10    0.001 ±  0.001    s/op
        SerializationBenchmark.protoStuffByteBuffer      ss      10   ≈ 10⁻⁴             s/op
        SerializationBenchmark.protoStuffBytesArray      ss      10   ≈ 10⁻⁴             s/op
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SerializationBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static final Serializer javaSerializer = SerializerFactory.getSerializer(SerializerType.JAVA.value());
    private static final Serializer hessianSerializer = SerializerFactory.getSerializer(SerializerType.HESSIAN.value());
    private static final Serializer protoStuffSerializer = SerializerFactory.getSerializer(SerializerType.PROTO_STUFF.value());
    private static final Serializer kryoSerializer = SerializerFactory.getSerializer(SerializerType.KRYO.value());

    private static final AdaptiveOutputBufAllocator.Handle allocHandle = AdaptiveOutputBufAllocator.DEFAULT.newHandle();
    private static final ByteBufAllocator allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());

    @Benchmark
    public void javaBytesArray() {
        byte[] bytes = javaSerializer.writeObject(createUserList());
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        javaSerializer.readObject(bytes, ArrayList.class);
    }

    @Benchmark
    public void javaByteBuffer() {
        OutputBuf outputBuf = javaSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUserList());
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.attach());
        javaSerializer.readObject(inputBuf, ArrayList.class);
    }

    @Benchmark
    public void hessianBytesArray() {
        byte[] bytes = hessianSerializer.writeObject(createUserList());
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        hessianSerializer.readObject(bytes, ArrayList.class);
    }

    @Benchmark
    public void hessianByteBuffer() {
        OutputBuf outputBuf = hessianSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUserList());
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.attach());
        hessianSerializer.readObject(inputBuf, ArrayList.class);
    }

    @Benchmark
    public void protoStuffBytesArray() {
        byte[] bytes = protoStuffSerializer.writeObject(createUserList());
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        protoStuffSerializer.readObject(bytes, ArrayList.class);
    }

    @Benchmark
    public void protoStuffByteBuffer() {
        OutputBuf outputBuf = protoStuffSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUserList());
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.attach());
        protoStuffSerializer.readObject(inputBuf, ArrayList.class);
    }

    @Benchmark
    public void kryoBytesArray() {
        byte[] bytes = kryoSerializer.writeObject(createUserList());
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        kryoSerializer.readObject(bytes, ArrayList.class);
    }

    @Benchmark
    public void kryoByteBuffer() {
        OutputBuf outputBuf = kryoSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUserList());
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.attach());
        kryoSerializer.readObject(inputBuf, ArrayList.class);
    }

    static final class NettyInputBuf implements InputBuf {

        private final ByteBuf byteBuf;

        NettyInputBuf(ByteBuf byteBuf) {
            this.byteBuf = byteBuf;
        }

        @Override
        public InputStream inputStream() {
            return new ByteBufInputStream(byteBuf); // should not be called more than once
        }

        @Override
        public ByteBuffer nioByteBuffer() {
            return byteBuf.nioBuffer(); // should not be called more than once
        }

        @Override
        public int size() {
            return byteBuf.readableBytes();
        }

        @Override
        public boolean release() {
            return byteBuf.release();
        }
    }

    static final class NettyOutputBuf implements OutputBuf {

        private final AdaptiveOutputBufAllocator.Handle allocHandle;
        private final ByteBuf byteBuf;
        private ByteBuffer nioByteBuffer;

        public NettyOutputBuf(AdaptiveOutputBufAllocator.Handle allocHandle, ByteBufAllocator alloc) {
            this.allocHandle = allocHandle;
            byteBuf = allocHandle.allocate(alloc);
        }

        @Override
        public OutputStream outputStream() {
            return new ByteBufOutputStream(byteBuf); // should not be called more than once
        }

        @Override
        public ByteBuffer nioByteBuffer(int minWritableBytes) {
            if (minWritableBytes < 0) {
                minWritableBytes = byteBuf.writableBytes();
            }

            if (nioByteBuffer == null) {
                nioByteBuffer = newNioByteBuffer(byteBuf, minWritableBytes);
            }

            if (nioByteBuffer.remaining() >= minWritableBytes) {
                return nioByteBuffer;
            }

            int position = nioByteBuffer.position();

            nioByteBuffer = newNioByteBuffer(byteBuf, position + minWritableBytes);

            nioByteBuffer.position(position);

            return nioByteBuffer;
        }

        @Override
        public int size() {
            if (nioByteBuffer == null) {
                return byteBuf.readableBytes();
            }
            return Math.max(byteBuf.readableBytes(), nioByteBuffer.position());
        }

        @Override
        public Object attach() {
            int actualWriteBytes = byteBuf.writerIndex();
            if (nioByteBuffer != null) {
                actualWriteBytes += nioByteBuffer.position();
            }

            allocHandle.record(actualWriteBytes);

            return byteBuf.writerIndex(actualWriteBytes);
        }

        private static ByteBuffer newNioByteBuffer(ByteBuf byteBuf, int writableBytes) {
            return byteBuf
                    .ensureWritable(writableBytes)
                    .nioBuffer(byteBuf.writerIndex(), byteBuf.writableBytes());
        }
    }

    static User createUser(int id) {
        User user = new User();
        user.setId(id);
        user.setName("block");
        user.setSex(0);
        user.setBirthday(new Date());
        user.setEmail("xxx@alibaba-inc.con");
        user.setMobile("18325038521");
        user.setAddress("浙江省 杭州市 文一西路969号");
        user.setPermissions(Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        return user;
    }

    static List<User> createUserList() {
        List<User> userList = Lists.newArrayListWithCapacity(10);
        for (int i = 0; i < 10; i++) {
            userList.add(createUser(i));
        }
        return userList;
    }

    static class User implements Serializable {

        private long id;
        private String name;
        private int sex;
        private Date birthday;
        private String email;
        private String mobile;
        private String address;
        private List<Integer> permissions;
        private int status;
        private Date createTime;
        private Date updateTime;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSex() {
            return sex;
        }

        public void setSex(int sex) {
            this.sex = sex;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public List<Integer> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<Integer> permissions) {
            this.permissions = permissions;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", sex=" + sex +
                    ", birthday=" + birthday +
                    ", email='" + email + '\'' +
                    ", mobile='" + mobile + '\'' +
                    ", address='" + address + '\'' +
                    ", permissions=" + permissions +
                    ", status=" + status +
                    ", createTime=" + createTime +
                    ", updateTime=" + updateTime +
                    '}';
        }
    }
}
