package Server;

import java.util.Arrays;

public final class ByteArray implements Comparable<ByteArray> {

    private byte[] array;
    private Integer arrayHash; //Dont re compute unless necessary
    private String arrayText; 

    public ByteArray(byte[] data) {
        array = data;
    }
    
    public void setArray(byte[] data) {
        array = data;
    }
    
    public byte[] getArray() {
        return array;
    }

    /**
     * Returns a memory independent copy of the underlying array of this
     * ByteArray.
     *
     * @return A memory independent copy of this ByteArray's internal array.
     */
    public byte[] getArrayCopy() {
        byte[] data = array; //avoid getfield opcode
        return Arrays.copyOf(data, data.length);
    }

    public void destroy() {
        array = null;
        arrayHash = null;
        arrayText = null;
    }

    @Override
    public String toString() {
        return arrayText != null ? arrayText : (arrayText = Arrays.toString(array));
    }

    public void updateHash() {
        arrayHash = null;
        arrayHash = Arrays.hashCode(array);
    }

    public void updateString() {
        arrayText = null;
        arrayText = Arrays.toString(array);
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
        return arrayHash != null ? arrayHash : (arrayHash = Arrays.hashCode(array));
    }
    
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ByteArray clone() {
        return new ByteArray(getArrayCopy());
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
}