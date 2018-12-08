package Server;

import java.util.Arrays;

//This class is not used anymore.
public final class ByteArray implements Comparable<ByteArray>, Cloneable, Recyclable {

    private static final int NULL_HASH = Integer.MIN_VALUE;
    
    private byte[] array;
    private int arrayHash = NULL_HASH; //Dont re compute unless necessary
    
    private ByteArray() {
        
    }
    
    //share reference with trusted resources only
    static ByteArray getLightWrapper(byte[] array) {
        ByteArray lightWrapper = new ByteArray();
        lightWrapper.array = array;
        return lightWrapper;
    }

    public ByteArray(byte[] data) {
        array = Arrays.copyOf(data, data.length);
    }
    
    public ByteArray(ByteArray other) {
        this(other.array);
    }
    
    public byte get(int index) {
        return array[index];
    }
    
    public void set(int index, byte value) {
        array[index] = value;
        arrayHash = NULL_HASH;
    }
    
    public int length() {
        return array.length;
    }
    
    /**
     * Returns a memory independent copy of the underlying array of this
     * ByteArray.
     *
     * @return A memory independent copy of this ByteArray's internal array.
     */
    public byte[] getArray() {
        byte[] data = array; //avoid getfield opcode
        return Arrays.copyOf(data, data.length);
    }

    public void setArray(byte[] data) {
        array = Arrays.copyOf(data, data.length);
        arrayHash = NULL_HASH;
    }
    
    //share reference with trusted resources only
    byte[] getInternalArray() {
        return array;
    }
    
    //share reference with trusted resources only
    void setInternalArray(byte[] data) {
        array = data;
        arrayHash = NULL_HASH;
    }

    @Override
    public void recycle() {
        array = null;
    }

    /**
     * Returns the value of the hash code of this ByteArray. Note: This method
     * will not automatically update the value of the hash code of the underlying array if it
     * the underlying array is externally modified. See {@link updateHash()} to update the hash code.
     *
     * @return
     */
    @Override
    public int hashCode() {
        int savedHash = arrayHash;
        byte[] arrayReference = array;
        return savedHash != NULL_HASH ? savedHash : (arrayHash = Arrays.hashCode(arrayReference));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ByteArray)) {
            return false;
        }
        return Arrays.equals(array, ((ByteArray) obj).array);
    }
    
    @Override
    public int compareTo(ByteArray other) {
        byte[] ourArray = array;
        byte[] otherArray = other.array;
        int ourLength = ourArray.length;
        int otherLength = otherArray.length;
        if (ourLength == otherLength) {
            for (int index = 0; index < ourLength; ++index) {
                byte ours = ourArray[index];
                byte theirs = otherArray[index];
                if (ours == theirs) {
                    continue;
                }
                return ours - theirs;
            }
            return 0;
        }
        else if (ourLength < otherLength) {
            for (int index = 0; index < ourLength; ++index) {
                byte ours = ourArray[index];
                byte theirs = otherArray[index];
                if (ours == theirs) {
                    continue;
                }
                return ours - theirs;
            }
            //our array is smaller, so it goes first
            return -1;
        }
        else {
            for (int index = 0; index < otherLength; ++index) {
                byte ours = ourArray[index];
                byte theirs = otherArray[index];
                if (ours == theirs) {
                    continue;
                }
                return ours - theirs;
            }
            //our array is larger
            return 1;
        }
    }
    
    @Override
    public String toString() {
        return Arrays.toString(array);
    }
    
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ByteArray clone() {
        return new ByteArray(this);
    }

    //test code to ensure no hash collisions
    public static void main(String[] args) {
        int nullHash = NULL_HASH;
        int[] bytes = new int[4];
        int count = 0;
        for (int a = Byte.MIN_VALUE; a <= Byte.MAX_VALUE; ++a) {
            bytes[0] = (byte) a;
            for (int b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; ++b) {
                bytes[1] = (byte) b;
                for (int c = Byte.MIN_VALUE; c <= Byte.MAX_VALUE; ++c) {
                    bytes[2] = (byte) c;
                    for (int d = Byte.MIN_VALUE; d <= Byte.MAX_VALUE; ++d) {
                        bytes[3] = (byte) d;
                        if (Arrays.hashCode(bytes) == nullHash) {
                            //System.out.println(Arrays.toString(bytes));
                            ++count;
                        }
                    }
                }
            }
        }
        System.out.println(count + " collisions with " + nullHash + " hash.");
    }
}