import React, { useState, useEffect } from 'react'

import {
  AsyncStorage,
  StyleSheet,
  Text,
  SafeAreaView,
  TouchableHighlight,
  View,
} from 'react-native'
import { styles as s } from 'react-native-style-tachyons'

import NativeTachyons from 'react-native-style-tachyons'

NativeTachyons.build(
  {
    colors: {
      palette: {
        white: '#FFF',
        'orange-100': '#FFFAF0',
        'orange-200': '#FEEBC8',
        'orange-300': '#FBD38D',
        'orange-400': '#F6AD55',
        'orange-500': '#ED8936',
        'orange-600': '#DD6B20',
        'orange-700': '#C05621',
        'orange-800': '#9C4221',
        'orange-900': '#7B341E',
        'grey-100': '#F7FAFC',
        'grey-200': '#EDF2F7',
        'grey-300': '#E2E8F0',
        'grey-400': '#CBD5E0',
        'grey-500': '#A0AEC0',
        'grey-600': '#718096',
        'grey-700': '#4A5568',
        'grey-800': '#2D3748',
        'grey-900': '#1A202C',
      },
    },
  },
  StyleSheet
)

export default function App() {
  const [counter, setCounter] = useState(154)

  useEffect(() => {
    AsyncStorage.getItem('counter').then((counter) =>
      setCounter(counter ? parseInt(counter) : 154)
    )
  }, [])

  return (
    <SafeAreaView style={[s.aic, s.jcsb, s.flx_i]}>
      <View>
        <View style={[s.mb5]}>
          <Text style={[s.f2, s.tc]}>💖</Text>
          <Text style={[s.f2, s.tc]}>
            Prinzessin's Ambulante Stunden Counter
          </Text>
          <Text style={[s.f2, s.tc]}>💖</Text>
        </View>
        <View style={[s.aic]}>
          <TouchableHighlight
            onPress={() =>
              AsyncStorage.setItem('counter', (counter + 1).toString()).then(
                () => setCounter(counter + 1)
              )
            }
            overlayColor={s.bg_orange_800.color}
            style={[s.bg_orange_500, s.w3, s.h3, s.jcc, s.aic, s.br4, s.mb4]}
          >
            <Text style={[s.f2, s.white]}>+</Text>
          </TouchableHighlight>
          <Text style={[s.f2, s.mb4]}>{counter}</Text>
          <TouchableHighlight
            onPress={() =>
              AsyncStorage.setItem('counter', (counter - 1).toString()).then(
                () => setCounter(counter - 1)
              )
            }
            overlayColor={s.bg_orange_800.color}
            style={[s.bg_orange_500, s.w3, s.h3, s.jcc, s.aic, s.br4, s.mb4]}
          >
            <Text style={[s.f2, s.white]}>-</Text>
          </TouchableHighlight>
        </View>
      </View>
      <View>
        <Text>made with 💖</Text>
      </View>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
})
